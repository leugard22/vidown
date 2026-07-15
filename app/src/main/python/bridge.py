import yt_dlp
import os

def fetch_info(url):
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'no_color': True,
        'noplaylist': True,
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)
        if not info:
            raise Exception("Failed to extract info")

        if 'entries' in info:
            entries = list(info.get('entries', []))
            if entries and entries[0]:
                info = entries[0]

        title = info.get('title') or info.get('playlist_title') or 'Unknown Title'
        thumbnail = info.get('thumbnail') or (info.get('thumbnails') and info.get('thumbnails')[0].get('url')) or ''
        uploader = info.get('uploader') or info.get('channel') or info.get('uploader_id') or 'Unknown Uploader'
        duration = info.get('duration', 0)
        extractor = info.get('extractor') or info.get('extractor_key') or 'Unknown'

        formats = info.get('formats', [])
        max_height = 0
        has_video = False

        for f in formats:
            h = f.get('height')
            if h is not None and f.get('vcodec') != 'none':
                has_video = True
                if h > max_height:
                    max_height = h

        if not max_height and info.get('height'):
            max_height = info.get('height')
            if max_height:
                has_video = True

        if not formats and info.get('url'):
            has_video = True

        res_formats = []
        if has_video:
            res_formats.append({'id': 'best', 'note': 'Best', 'ext': 'mp4', 'is_audio': False})
            if max_height >= 1080:
                res_formats.append({'id': '1080p', 'note': '1080p', 'ext': 'mp4', 'is_audio': False})
            if max_height >= 720:
                res_formats.append({'id': '720p', 'note': '720p', 'ext': 'mp4', 'is_audio': False})
            if max_height >= 480:
                res_formats.append({'id': '480p', 'note': '480p', 'ext': 'mp4', 'is_audio': False})
            if max_height >= 360:
                res_formats.append({'id': '360p', 'note': '360p', 'ext': 'mp4', 'is_audio': False})

        res_formats.append({'id': 'audio', 'note': 'Audio', 'ext': 'mp3', 'is_audio': True})

        return {
            'title': title,
            'thumbnail': thumbnail,
            'uploader': uploader,
            'duration': duration if duration is not None else 0,
            'extractor': extractor,
            'formats': res_formats,
        }

def download(url, format_preset, output_path, ffmpeg_dir, callback, cookies_path=None, embed_subtitles=False):
    format_map = {
        'best': 'best',
        '1080p': 'bestvideo[height<=1080]+bestaudio/best',
        '720p': 'bestvideo[height<=720]+bestaudio/best',
        '480p': 'bestvideo[height<=480]+bestaudio/best',
        'audio': 'bestaudio/best'
    }

    selected_format = format_map.get(format_preset, 'bestvideo+bestaudio/best')

    def _hook(d):
        if d['status'] == 'downloading':
            total = d.get('total_bytes') or d.get('total_bytes_estimate') or 1
            downloaded = d.get('downloaded_bytes', 0)
            pct = int((downloaded / total) * 100)
            callback.onProgress(pct, 'DOWNLOADING')
        elif d['status'] == 'finished':
            callback.onProgress(100, 'MERGING')

    opts = {
        'format': selected_format,
        'outtmpl': output_path,
        'ffmpeg_location': ffmpeg_dir,
        'progress_hooks': [_hook],
        'quiet': True,
        'no_warnings': True,
        'no_color': True,
        'noplaylist': True,
    }

    if cookies_path and os.path.exists(cookies_path):
        opts['cookiefile'] = cookies_path

    if format_preset != 'audio':
        opts['merge_output_format'] = 'mp4/mkv'

    postprocessors = []

    if format_preset == 'audio':
        postprocessors.append({
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        })

    if embed_subtitles:
        opts['writesubtitles'] = True
        opts['subtitleslangs'] = ['en', 'all']
        postprocessors.append({
            'key': 'FFmpegEmbedSubtitle',
            'already_have_subtitle': False,
        })

    if postprocessors:
        opts['postprocessors'] = postprocessors

    with yt_dlp.YoutubeDL(opts) as ydl:
        ydl.download([url])

def init_update_path(path):
    import sys
    import os
    if path and os.path.exists(path):
        if path in sys.path:
            sys.path.remove(path)
        sys.path.insert(0, path)
        for mod in list(sys.modules.keys()):
            if mod.startswith('yt_dlp'):
                del sys.modules[mod]
