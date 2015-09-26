package moe.nightfall.instrumentality

/**
 * Created on 26/09/15.
 */
object RecommendedInfoCache {
    val recommended = scala.io.Source.fromInputStream(classOf[Main].getClassLoader.getResourceAsStream("assets/instrumentality/posesbuiltin/recommended.csv"))
    val allEntries = recommended.getLines.toList.tail.map(new DownloadableEntry(_))
    recommended.close()
    var availableEntries = allEntries.filter(!_.isInstalled)

    def refreshAvailable = availableEntries = allEntries.filter(!_.isInstalled)

    class DownloadableEntry(text : String) {
        val arr = text.split(",")
        
        val sha = arr(0)
        val name = arr(1)
        val author = arr(2)
        val poser = arr(3)

        def isInstalled: Boolean = ModelCache.localFromHash(sha).isDefined
    }
}
