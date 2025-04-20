package io.github.uditkarode.able.fragments

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.uditkarode.able.R
import io.github.uditkarode.able.adapters.YtResultAdapter
import io.github.uditkarode.able.adapters.YtmResultAdapter
import io.github.uditkarode.able.databinding.SearchBinding
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.utils.Shared
import io.github.uditkarode.able.utils.SwipeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.lang.ref.WeakReference
import java.util.Collections.singletonList

/**
 * The second fragment. Used to search for songs.
 */
@ExperimentalCoroutinesApi
class Search : Fragment(), CoroutineScope {
    private lateinit var itemPressed: SongCallback
    private lateinit var sp: SharedPreferences
    private var searchJob: kotlinx.coroutines.Job? = null

    companion object {
        val resultArray = ArrayList<Song>()
    }

    interface SongCallback {
        fun sendItem(song: Song, mode: String = "")
    }

    override val coroutineContext = Dispatchers.Main + SupervisorJob()
    private var _binding: SearchBinding? = null

    private val binding get() = _binding!!

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            itemPressed = context as Activity as SongCallback
        } catch (e: ClassCastException) {
            throw ClassCastException(
                activity.toString()
                        + " must implement SongCallback"
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sp = requireContext().getSharedPreferences("search", 0)

        when (sp.getString("mode", "Music")) {
            "Album" -> {
                _binding!!.searchMode.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.mode_album
                    )
                )
            }
            "Playlists" -> {
                _binding!!.searchMode.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.mode_playlist
                    )
                )
            }
        }

        View.OnClickListener {
            when (sp.getString("mode", "Music")) {
                "Music" -> {
                    sp.edit().putString("mode", "Album").apply()
                    _binding!!.searchMode.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.mode_album
                        )
                    )
                }
                "Album" -> {
                    sp.edit().putString("mode", "Playlists").apply()
                    _binding!!.searchMode.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.mode_playlist
                        )
                    )
                }
                "Playlists" -> {
                    sp.edit().putString("mode", "Music").apply()
                    _binding!!.searchMode.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.mode_music
                        )
                    )
                }
            }
        }.also {
            _binding!!.searchMode.setOnClickListener(it)
            _binding!!.searchModePr.setOnClickListener(it)
        }
        _binding!!.loadingView.enableMergePathsForKitKatAndAbove(true)
        setupSearch(view.findViewById(R.id.search_bar), view.findViewById(R.id.search_rv))
    }

    private fun setupSearch(searchBar: EditText, searchRv: RecyclerView) {
        // Add text watcher for live search
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel() // Cancel previous search if any
                
                val query = s.toString().trim()
                if (query.length >= 3) { // Only search when query has at least 3 characters
                    searchJob = launch {
                        delay(500) // Add debounce to avoid too many requests
                        performSearch(query, searchBar, searchRv, isLiveSearch = true)
                    }
                } else if (query.isEmpty()) {
                    // Clear results if search is empty
                    resultArray.clear()
                    searchRv.adapter?.notifyDataSetChanged()
                }
            }
        })

        // Original editor action listener for explicit search
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == 6) {
                val query = searchBar.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query, searchBar, searchRv, isLiveSearch = false)
                }
            }
            false
        }
    }

    private fun performSearch(
        query: String,
        searchBar: EditText,
        searchRv: RecyclerView,
        isLiveSearch: Boolean
    ) {
        if (!Shared.isInternetConnected(requireContext())) {
            Toast.makeText(requireContext(), "No Internet Connection", Toast.LENGTH_LONG).show()
            return
        }

        if (!isLiveSearch) {
            _binding!!.loadingView.progress = 0.3080229f
            _binding!!.loadingView.playAnimation()

            if (searchRv.visibility == View.VISIBLE) {
                searchRv.animate().alpha(0f).duration = 200
                searchRv.visibility = View.GONE
            }

            hideKeyboard(activity as Activity)
            if (_binding!!.loadingView.visibility == View.GONE) {
                _binding!!.loadingView.alpha = 0f
                _binding!!.loadingView.visibility = View.VISIBLE
                _binding!!.loadingView.animate().alpha(1f).duration = 200
            }
        }

        resultArray.clear()
        
        launch(Dispatchers.IO) {
            val useYtMusic = when {
                query.startsWith("!") -> {
                    val modifiedQuery = query.replaceFirst(Regex("^!\\s*"), "")
                    true
                }
                query.startsWith("?") -> {
                    val modifiedQuery = query.replaceFirst(Regex("^?\\s*"), "")
                    false
                }
                else -> (PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("source_key", "Youtube Music") == "Youtube Music")
            }

            try {
                if (useYtMusic) {
                    when (sp.getString("mode", "Music")) {
                        "Music" -> {
                            val extractor = YouTube.getSearchExtractor(
                                query, singletonList(
                                    YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
                                ), ""
                            )
                            extractor.fetchPage()

                            for (song in extractor.initialPage.items) {
                                val ex = song as StreamInfoItem
                                var thumbnailUrl = ex.thumbnails.firstOrNull()?.url ?: ""
                                if (thumbnailUrl.isNotEmpty()) {
                                    // Use medium quality thumbnail for faster loading
                                    if (thumbnailUrl.contains("ytimg")) {
                                        val songId = Shared.getIdFromLink(ex.url)
                                        thumbnailUrl = "https://i.ytimg.com/vi/$songId/mqdefault.jpg"
                                    }
                                }
                                resultArray.add(
                                    Song(
                                        name = ex.name,
                                        artist = ex.uploaderName,
                                        youtubeLink = ex.url,
                                        ytmThumbnail = thumbnailUrl
                                    )
                                )
                            }
                        }
                        "Album" -> {
                            val extractor = YouTube.getSearchExtractor(
                                query, singletonList(
                                    YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS
                                ), ""
                            )
                            extractor.fetchPage()
                            for (song in extractor.initialPage.items) {
                                val ex = song as PlaylistInfoItem
                                var thumbnailUrl = ex.thumbnails.firstOrNull()?.url ?: ""
                                if (thumbnailUrl.isNotEmpty()) {
                                    if (thumbnailUrl.contains("ytimg")) {
                                        val songId = Shared.getIdFromLink(ex.url)
                                        thumbnailUrl = "https://i.ytimg.com/vi/$songId/mqdefault.jpg"
                                    }
                                }
                                resultArray.add(
                                    Song(
                                        name = ex.name,
                                        artist = ex.uploaderName,
                                        youtubeLink = ex.url,
                                        ytmThumbnail = thumbnailUrl
                                    )
                                )
                            }
                        }
                        "Playlists" -> {
                            val extractor = if (query.startsWith("https://"))
                                YouTube.getPlaylistExtractor(query)
                            else
                                YouTube.getSearchExtractor(
                                    query, singletonList(
                                        YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS
                                    ), ""
                                )
                            extractor.fetchPage()
                            for (song in extractor.initialPage.items) {
                                val ex = song as PlaylistInfoItem
                                var thumbnailUrl = ex.thumbnails.firstOrNull()?.url ?: ""
                                if (thumbnailUrl.isNotEmpty()) {
                                    if (thumbnailUrl.contains("ytimg")) {
                                        val songId = Shared.getIdFromLink(ex.url)
                                        thumbnailUrl = "https://i.ytimg.com/vi/$songId/mqdefault.jpg"
                                    }
                                }
                                resultArray.add(
                                    Song(
                                        name = ex.name,
                                        artist = ex.uploaderName,
                                        youtubeLink = ex.url,
                                        ytmThumbnail = thumbnailUrl
                                    )
                                )
                            }
                        }
                    }
                } else {
                    val extractor = YouTube.getSearchExtractor(
                        query, singletonList(
                            YoutubeSearchQueryHandlerFactory.VIDEOS
                        ), ""
                    )
                    extractor.fetchPage()
                    for (song in extractor.initialPage.items) {
                        val ex = song as StreamInfoItem
                        var thumbnailUrl = ex.thumbnails.firstOrNull()?.url ?: ""
                        if (thumbnailUrl.isNotEmpty()) {
                            // For regular YouTube videos, use the medium quality thumbnail
                            if (thumbnailUrl.contains("ytimg")) {
                                val songId = Shared.getIdFromLink(ex.url)
                                thumbnailUrl = "https://i.ytimg.com/vi/$songId/mqdefault.jpg"
                            }
                        }
                        resultArray.add(
                            Song(
                                name = ex.name,
                                artist = ex.uploaderName,
                                youtubeLink = ex.url,
                                ytmThumbnail = thumbnailUrl
                            )
                        )
                    }
                }

                launch(Dispatchers.Main) {
                    if (useYtMusic) {
                        searchRv.adapter =
                            YtmResultAdapter(
                                resultArray,
                                WeakReference(this@Search),
                                sp.getString("mode", "Music") ?: "Music"
                            )
                    } else {
                        searchRv.adapter =
                            YtResultAdapter(resultArray, WeakReference(this@Search))
                    }
                    
                    if (!isLiveSearch) {
                        searchRv.layoutManager = LinearLayoutManager(requireContext())
                        _binding!!.loadingView.visibility = View.GONE
                        _binding!!.loadingView.pauseAnimation()
                        searchRv.alpha = 0f
                        searchRv.visibility = View.VISIBLE
                        searchRv.animate().alpha(1f).duration = 200
                        val itemTouchHelper = ItemTouchHelper(
                            SwipeController(
                                context,
                                "Search",
                                null
                            )
                        )
                        itemTouchHelper.attachToRecyclerView(searchRv)
                    } else {
                        searchRv.adapter?.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    if (!isLiveSearch) {
                        Toast.makeText(requireContext(), "Something failed!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    fun itemPressed(song: Song) {
        if (Shared.isInternetConnected(requireContext()))
            itemPressed.sendItem(song)
        else
            Toast.makeText(requireContext(), "No Internet Connection", Toast.LENGTH_LONG).show()
    }

    private fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: View(activity)

        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}