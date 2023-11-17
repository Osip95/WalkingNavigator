package com.example.walkingnavigator


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walkingnavigator.databinding.ActivitySearchBinding
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SuggestItem
import com.yandex.mapkit.search.SuggestOptions
import com.yandex.mapkit.search.SuggestSession
import com.yandex.mapkit.search.SuggestType
import com.yandex.runtime.Error


class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var suggestSession: SuggestSession
    private lateinit var searchManager: SearchManager
    val adapter  by lazy {
        SearchAdapter { text, latitude, longitude ->
            goToMainActivity(text,latitude,longitude)
        }
    }
    private val suggestListener = object : SuggestSession.SuggestListener {
        override fun onResponse(p0: MutableList<SuggestItem>) {
            adapter.setData(p0)
        }
        override fun onError(p0: Error) {
            Toast.makeText(applicationContext, "onError", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val map = (application as App).getMap()
        binding.rvResultSearch.adapter = adapter
        binding.rvResultSearch.layoutManager = LinearLayoutManager(this)
        searchManager = SearchFactory.getInstance().createSearchManager(
            SearchManagerType.COMBINED
        )
        suggestSession = searchManager.createSuggestSession()
        val suggestOptions = SuggestOptions()
            .setSuggestTypes(
                SuggestType.UNSPECIFIED.value
            )

        binding.etSearch.doAfterTextChanged { text ->
            suggestSession.suggest(
                text.toString(),
                BoundingBox(
                    map.visibleRegion.bottomLeft,
                    map.visibleRegion.topRight
                ),
                suggestOptions,
                suggestListener,
            )
        }
    }

    private fun goToMainActivity(text:String,latitude:Double?,longitude:Double? ){
        val intent = Intent(this@SearchActivity,MainActivity::class.java)
        intent.putExtra("text", text)
        intent.putExtra("latitude",latitude)
        intent.putExtra("longitude",longitude)
        startActivity(intent)
    }
}