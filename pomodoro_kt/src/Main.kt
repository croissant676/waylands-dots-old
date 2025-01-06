import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread
import kotlin.math.roundToInt

// simple pomodoro server
data class Session(var desc: String, val duration: Int)

fun writeDuration(duration: Int): String {
    var remaining = duration
    val text = StringBuilder()
    if (duration > 3600) {
        text.append(duration / 3600).append(':')
        remaining %= 3600
    }
    val mins = remaining / 60
    if (mins < 10) text.append('0')
    text.append(mins).append(':')
    val seconds = remaining % 60
    if (seconds < 10) text.append('0')
    text.append(seconds)
    return text.toString()
}

var sessionList = buildList {
    repeat(4) {
        add(Session("work", 25 * 60))
        add(Session("break", 5 * 60))
    }
    add(Session("break", 30 * 60))
}

var curIndex = 0
var sessionInfo: Long = Long.MIN_VALUE
val curSession: Session?
    get() {
        if (sessionInfo == Long.MIN_VALUE) return null
        return sessionList[curIndex]
    }

fun initializeSessionInfo() {
    sessionInfo = System.currentTimeMillis()
}

fun endSession() {
    val nextSession = sessionList[(curIndex + 1) % sessionList.size]
    Runtime.getRuntime().exec(
        arrayOf(
            "notify-send",
            "pomodoro session complete!",
            "next up: ${nextSession.desc} (${writeDuration(nextSession.duration)})"
        )
    )
    val clip = AudioSystem.getClip()
    val inputStream: AudioInputStream = AudioSystem.getAudioInputStream(File("/home/kason/Downloads/mixkit-correct-answer-tone-2870.wav"))
    clip.open(inputStream)
    clip.start()
}

fun start() {
    if (curSession == null) {
        println("initializing session info")
        initializeSessionInfo()
    }
}

fun stop() {
    sessionInfo = Long.MIN_VALUE
    curIndex = 0
}

fun nextSession() {
    curIndex += 1
    curIndex %= sessionList.size
    initializeSessionInfo()
}

fun jsonWaybar(text: String, tooltip: String = createTooltipText()): String =
    """{"text":"$text","tooltip":"$tooltip"}"""

fun surroundSpan(text: String, color: String) = "<span color=\"$color\">$text</span>"

fun createTooltipText(): String {
    val stringBuilder = StringBuilder("current session list\n")
    for (sessionIndex in sessionList.indices) {
        if (sessionIndex == curIndex) {
            stringBuilder.append("<span color=\"#85c1dc\">> ")
        } else {
            stringBuilder.append("<span color=\"#a5adce\">")
            stringBuilder.append(sessionIndex + 1).append('.')
        }
        val session = sessionList[sessionIndex]
        stringBuilder.append('[').append(writeDuration(session.duration)).append("] ")
        stringBuilder.appendLine(session.desc)
        stringBuilder.append("</span>")
    }

    stringBuilder.appendLine()
    stringBuilder.appendLine(surroundSpan("actions","#ef9f76"))
    stringBuilder.appendLine(surroundSpan(":left-click to toggle pause / unpause","#ef9f76"))
    stringBuilder.appendLine(surroundSpan(":right-click to toggle stop","#ef9f76"))
    stringBuilder.appendLine(surroundSpan(":scroll-up to move on to next task","#ef9f76"))
    stringBuilder.appendLine(surroundSpan(":scroll-down to move to prev task or reset","#ef9f76"))
    stringBuilder.appendLine(surroundSpan(":scroll-click to update session list","#ef9f76"))
    stringBuilder.appendLine()

    stringBuilder.append(surroundSpan("session list file: ${file.absolutePath}", "#ca9ee6"))
    return stringBuilder.toString()
        .replace("\n", "\\n")
        .replace("\"", "\\\"")
}

fun poll(): String {
    if (sessionList.isEmpty() || curSession == null) {
        return jsonWaybar("no active session", surroundSpan(":click to start the session", "#e78284")
            .replace("\n", "\\n")
            .replace("\"", "\\\""))
    }
    val sessionTime = writeDuration(curSession!!.duration)
    if (sessionInfo < 0) {
        val timeLeft = writeDuration((-sessionInfo).toInt())
        return jsonWaybar("${curIndex + 1} ${curSession!!.desc} ${timeLeft}/${sessionTime} (paused)")
    } else {
        val sDiff = ((System.currentTimeMillis() - sessionInfo) / 1000.0).roundToInt()
        val duration = curSession!!.duration - sDiff
        if (duration < 0) {
            endSession()
            nextSession()
            return poll()
        } else {
            val timeLeft = writeDuration(duration)
            return jsonWaybar("${curIndex + 1} ${curSession!!.desc} ${timeLeft}/${sessionTime}")
        }
    }
}

fun togglePause() {
    if (sessionInfo == Long.MIN_VALUE)
        return start()
    if (sessionInfo < 0) {
        val timeLapsed = curSession!!.duration - (-sessionInfo).toInt()
        sessionInfo = System.currentTimeMillis() - timeLapsed * 1000
    } else {
        val timeLapsed = ((System.currentTimeMillis() - sessionInfo) / 1000.0).roundToInt()
        val timeLeft = curSession!!.duration - timeLapsed
        sessionInfo = (-timeLeft).toLong()
    }
}

fun parseSessionList(string: String): List<Session> = buildList {
    for (s in string.split("\n")) {
        val tokens = s.split("\t").map { it.trim() }
        add(Session(tokens.getOrNull(0) ?: continue, (tokens.getOrNull(1) ?: continue).toInt()))
    }
}

fun sessionListToString(): String {
    val stringBuilder = StringBuilder()
    for (session in sessionList) {
        stringBuilder.append(session.desc).append("\t").appendLine(session.duration)
    }
    return stringBuilder.toString()
}

fun scrollDown() {
    if (sessionList.isEmpty() || curSession == null) return
    val timeLapsed = if (sessionInfo < 0) {
        curSession!!.duration - (-sessionInfo).toInt()
    } else {
        ((System.currentTimeMillis() - sessionInfo) / 1000.0).roundToInt()
    }
    if (timeLapsed < 5) { // set the prev session
        curIndex -= 1
        if (curIndex < 0) {
            curIndex = sessionList.lastIndex
        }
    }
    initializeSessionInfo()
}

val file = File("session_list.data")
fun main() {
    if (!file.exists()) {
        file.createNewFile()
    } else {
        val text = file.readText()
        if (text.isNotBlank()) {
            sessionList = parseSessionList(file.readText())
        }
    }
    val server = HttpServer.create(InetSocketAddress("localhost", 9081), 0)
    server.createContext("/poll") {
        it.sendResponseHeaders(200, 0)
        it.responseHeaders["Content-Type"] = "application/json"
        val response = poll()
        it.responseBody?.write(response.toByteArray())
        it.responseBody?.close()
    }
    server.createContext("/lc") {
        if (curSession == null) start()
        else togglePause()
    }
    server.createContext("/stop") {
        stop()
        file.writeText(sessionListToString())
    }
    server.createContext("/next") { nextSession() }
    server.createContext("/sc_down") {  
        scrollDown()
    }
    server.createContext("/reset") {
        initializeSessionInfo()
    }
    server.createContext("/reload") {
        sessionList = parseSessionList(file.readText())
        curIndex = 0
        initializeSessionInfo()
    }
    server.executor = Executors.newCachedThreadPool()
    server.start()
    println("file: ${file.absolutePath}")
    println("server started!")
    
    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        file.writeText(sessionListToString())
    })
}