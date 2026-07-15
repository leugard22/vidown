package app.vidown.data.python

interface ProgressCallback {
    fun onProgress(percent: Int, status: String)
}
