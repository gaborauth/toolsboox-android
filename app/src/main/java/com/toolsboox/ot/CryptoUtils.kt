package com.toolsboox.ot

import java.security.SecureRandom
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
        val random = SecureRandom()

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
