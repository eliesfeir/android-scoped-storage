package com.example.scopedstorage.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.scopedstorage.R
import com.example.scopedstorage.fragments.MediaStoreFragment
import com.example.scopedstorage.fragments.NonMediaFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupTabs()
    }

    fun setupTabs() {


        viewPager.adapter = object : FragmentStateAdapter(this) {

            override fun createFragment(position: Int): Fragment {
                if (position == 0) {
                    return MediaStoreFragment()
                } else {
                    return NonMediaFragment()
                }
            }

            override fun getItemCount(): Int {
                return 2
            }

        }

        TabLayoutMediator(tabindicator, viewPager) { tab, position ->
            tab.text =
                if (position == 0) getString(R.string.media) else getString(R.string.non_media)
        }.attach()

        viewPager.isUserInputEnabled = true

    }

}
