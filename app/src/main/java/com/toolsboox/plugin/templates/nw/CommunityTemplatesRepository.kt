package com.toolsboox.plugin.templates.nw

import com.toolsboox.plugin.templates.da.CommunityTemplate

/**
 * The 'community templates' repository.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CommunityTemplatesRepository {

    companion object {
        /**
         * Cached map.
         */
        private val communityTemplates: MutableMap<String, CommunityTemplate> = mutableMapOf()
    }


    /**
     * Get the cached object.
     *
     * @param name
     * @return the object
     */
    fun get(name: String): CommunityTemplate? {
        return communityTemplates[name]
    }

    /**
     * Get the cached list.
     *
     * @return the list
     */
    fun list(): List<CommunityTemplate> {
        return communityTemplates.values.toList()
    }

    /**
     * Update the cached list.
     *
     * @param newObjects the new object list
     */
    fun updateList(newObjects: List<CommunityTemplate>) {
        communityTemplates.clear()
        newObjects.forEach {
            communityTemplates[it.name] = it
        }
    }
}
