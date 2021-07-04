package com.penguinstudio.safecrypt.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.penguinstudio.safecrypt.ui.home.GalleryFragment

enum class GalleryType {
    NORMAL, ENCRYPTED
}
class HomeTabPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = GalleryFragment()

        // It recreates fragment but keeps the ViewModel
        fragment.arguments = Bundle().apply {
            if(position == 0) {
                putSerializable("GalleryType", GalleryType.NORMAL)
            }
            else {
                putSerializable("GalleryType", GalleryType.ENCRYPTED)
            }
        }

        return fragment
    }
}