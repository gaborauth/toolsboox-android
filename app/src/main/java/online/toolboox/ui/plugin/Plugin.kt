package online.toolboox.ui.plugin

/**
 * Interface of plugins.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface Plugin {
    /**
     * Get the fragment of the URL.
     *
     * @param url the URL
     */
    fun getRoute(url: String): ScreenFragment?
}
