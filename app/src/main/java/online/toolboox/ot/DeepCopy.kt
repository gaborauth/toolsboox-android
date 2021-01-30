package online.toolboox.ot

import com.google.gson.Gson

/**
 * Gson based deep copy.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class DeepCopy {
    companion object {

        /**
         * Deep copy of the object (object - JSON - object coversion).
         *
         * @param t the object
         * @return the deep copied object
         */
        inline fun <reified T> deepCopy(t: T): T {
            val json = Gson().toJson(t)
            return Gson().fromJson(json, T::class.java)
        }
    }
}
