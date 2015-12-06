/*
 * Copyright (c) 2015, Nightfall Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package moe.nightfall.instrumentality

/**
 * Created on 26/09/15.
 */
object RecommendedInfoCache {
    var isLoaded = false
    var allEntries = Seq[DownloadableEntry]()
    var availableEntries = allEntries

    def loadRecommended = {
        val recommended = scala.io.Source.fromInputStream(Loader.applicationHost.getResource("posesbuiltin/recommended.csv"))
        allEntries = recommended.getLines.toList.tail.map(new DownloadableEntry(_))
        recommended.close
        refreshAvailable
        isLoaded = true
    }

    def refreshAvailable = availableEntries = allEntries.filter(!_.isInstalled)

    class DownloadableEntry(text : String) {
        val arr = text.split(",")
        
        val sha = arr(0)
        val name = arr(1)
        val author = arr(2)
        val poser = arr(3)
        val download = arr(4)
        val downloadDir = arr(5)

        def isInstalled: Boolean = ModelCache.localFromHash(sha).isDefined
    }
}
