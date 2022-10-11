package online.toolboox.ui.plugin

/**
 * The router interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface Router {
    /**
     * Dispatch the specified URL.
     *
     * @param url the URL
     */
    fun dispatch(url: String, replace: Boolean = false)

    /**
     * Extract parameters from the URLs based on the patterns.
     */
    fun getParameters(pattern: String, url: String): Parameters

    /**
     * The parameter matcher result
     */
    sealed class Parameters {
        class Match(val parameters: Map<String, String>) : Parameters()
        class NoMatch(val parameters: Map<String, String>) : Parameters()
    }
}
