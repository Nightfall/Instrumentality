package moe.nightfall.instrumentality

/**
 * Created on 26/09/15.
 */
object RecommendedInfoCache {
    val recommended = scala.io.Source.fromInputStream(classOf[Main].getClassLoader.getResourceAsStream("assets/instrumentality/posesbuiltin/recommended.txt"))
    val allEntries = recommended.getLines.filter(_.startsWith(":")).map(new DownloadableEntry(_)).toList
    recommended.close()
    var availableEntries = allEntries.filter(!_.isInstalled)

    def refreshAvailable = availableEntries = allEntries.filter(!_.isInstalled)

    class DownloadableEntry() {
        var sha = ""
        var name = ""
        var author = ""
        var poser = ""

        def this(text: String) {
            this
            val arr = text.split(":").tail.map(_.trim)
            sha = arr(0)
            name = arr(1)
            author = arr(2)
            poser = arr(3)
        }

        def isInstalled: Boolean = ModelCache.localFromHash(sha).isDefined

    }

}
