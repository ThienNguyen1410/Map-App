package com.map.kotlin.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.map.kotlin.model.Bookmark
import com.map.kotlin.repository.BookmarkRepo
import com.map.kotlin.util.ImageUtils

class MapsViewModel(application: Application) :
    AndroidViewModel(application) {
    private val TAG = "MapsViewModel"
    private var bookmarks: LiveData<List<BookmarkView>>? = null

    private var bookmarkRepo: BookmarkRepo = BookmarkRepo(
        getApplication()
    )

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()
        bookmark.category = getPlaceCategory(place)
        val newId = bookmarkRepo.addBookmark(bookmark)
        image?.let { bookmark.setImage(getApplication(), it) }
        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    data class BookmarkView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0),
        var name: String = "",
        var address: String = "",
        var phone: String = "",
        var categoryResourceId: Int? = null
    ) {
        fun getImage(context: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(
                    context,
                    Bookmark.generateImageFileName(it)
                )
            }
            return null
        }
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkView {
        return BookmarkView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.address,
            bookmark.phone,
            bookmarkRepo.getCategoryResourceId(bookmark.category)

        )
    }

    private fun mapBookmarkToBookmarkView() {
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks)
        { repoBookmarks ->
            repoBookmarks.map { bookmark ->
                bookmarkToBookmarkView(bookmark)
            }
        }
    }

    fun getBookmarkViews():
            LiveData<List<BookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarkToBookmarkView()
        }
        return bookmarks
    }

    private fun getPlaceCategory(place: Place): String {
        var category = "Other"
        val placeTypes = place.types

        if (placeTypes != null) {
            if (placeTypes.size > 0) {
                val placeType = placeTypes[0]
                category = bookmarkRepo.placeTypeToCategory(placeType)
            }
        }
        return category
    }

    fun addBookmark (latlng : LatLng): Long? {
        val bookmark =  bookmarkRepo.createBookmark()
        bookmark.name = "Untitled"
        bookmark.longitude = latlng.longitude
        bookmark.latitude = latlng.latitude
        bookmark.category = "Other"
        return bookmarkRepo.addBookmark(bookmark)
    }
}

