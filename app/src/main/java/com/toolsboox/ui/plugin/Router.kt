package com.toolsboox.ui.plugin

import com.google.code.regexp.Pattern

/**
 * The router interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface Router {
    companion object {
        /**
         * Return with the parameter of the URL.
         */
        fun getParameters(pattern: String, url: String): Parameters =
            Pattern.compile(pattern).matcher(url).let {
                when {
                    it.matches() && it.groupCount() > 0 -> {
                        Parameters.Match(it.namedGroups().firstOrNull() ?: mapOf())
                    }

                    it.matches() -> {
                        Parameters.Match(mapOf())
                    }

                    else -> {
                        Parameters.NoMatch(mapOf())
                    }
                }
            }
    }

    /**
     * Dispatch the specified URL.
     *
     * @param url the URL
     */
    fun dispatch(url: String, replace: Boolean = false)

    /**
     * The parameter matcher result
     */
    sealed class Parameters {
        class Match(val parameters: Map<String, String>) : Parameters()
        class NoMatch(val parameters: Map<String, String>) : Parameters()
    }
}
