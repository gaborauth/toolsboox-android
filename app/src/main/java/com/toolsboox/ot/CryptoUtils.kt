package com.toolsboox.ot

import timber.log.Timber
import java.math.BigInteger
import java.nio.BufferOverflowException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptography utilities.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CryptoUtils {
    companion object {
        /**
         * The secure random instance.
         */
        private val random = SecureRandom()

        /**
         * Decrypt the encrypted message with the password.
         *
         * @param encrypted the encrypted message
         * @param password the password
         * @return the decrypted message
         */
        fun decrypt(encrypted: ByteArray, password: String): ByteArray {
            val salt = encrypted.copyOfRange(8, 16)
            val cipherText = encrypted.copyOfRange(16, encrypted.size)

            val keySpecs = createKeySpecs(password, salt)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpecs.secretKey, keySpecs.ivSpec)
            return cipher.doFinal(cipherText)
        }

        /**
         * Encrypt the clear message with the password.
         *
         * @param clear the clear message
         * @param password the password
         * @return the encrypted message
         */
        fun encrypt(clear: ByteArray, password: String): ByteArray {
            val keySpecs = createKeySpecs(password)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpecs.secretKey, keySpecs.ivSpec)

            return "Salted__".toByteArray(Charsets.UTF_8) + keySpecs.salt + cipher.doFinal(clear)
        }

        /**
         * Create MD5 hash of the byte array.
         *
         * @param clear the clear byte array
         * @return the md5 hash in hex string
         */
        fun md5Hash(clear: ByteArray): String {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(clear)
            return BigInteger(1, md5.digest()).toString(16)
        }

        /**
         * Get condensed key based on the UUID.
         *
         * @param uuid the UUID
         * @return the condensed key
         */
        fun getKey(uuid: UUID?): String? {
            if (uuid == null) {
                return null
            }

            val buffer = ByteBuffer.allocate(java.lang.Long.BYTES * 2)
            buffer.putLong(uuid.leastSignificantBits)
            buffer.putLong(uuid.mostSignificantBits)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array())
        }

        /**
         * Get the UUID based of the condensed key.
         *
         * @param key the condensed key
         * @return the UUID
         */
        fun getUUID(key: String?): UUID? {
            if (key == null) {
                return null
            }

            try {
                val array = Base64.getUrlDecoder().decode(key)
                val buffer = ByteBuffer.wrap(array)
                val low = buffer.long
                val high = buffer.long
                return UUID(high, low)
            } catch (ex: IllegalArgumentException) {
                Timber.i(ex.message)
            } catch (ex: BufferOverflowException) {
                Timber.i(ex.message)
            } catch (ex: BufferUnderflowException) {
                Timber.i(ex.message)
            }

            return null
        }

        /**
         * Create PBKDF2 compatible key and IV from a password and an optional salt.
         *
         * @param password the password
         * @param optionalSalt the optional salt
         * @return the KeySpecs instance
         */
        private fun createKeySpecs(password: String, optionalSalt: ByteArray? = null): KeySpecs {
            val salt = if (optionalSalt == null) {
                val generatedSalt = ByteArray(8)
                random.nextBytes(generatedSalt)
                generatedSalt
            } else {
                optionalSalt
            }

            val keySpec = PBEKeySpec(password.toCharArray(), salt, 10000, 384)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyAndIv = factory.generateSecret(keySpec)
            val key = keyAndIv.encoded.copyOfRange(0, 32)
            val iv = keyAndIv.encoded.copyOfRange(32, 48)

            return KeySpecs(SecretKeySpec(key, "AES"), IvParameterSpec(iv), salt)
        }
    }

    data class KeySpecs(
        val secretKey: SecretKeySpec,
        val ivSpec: IvParameterSpec,
        val salt: ByteArray
    )
}
