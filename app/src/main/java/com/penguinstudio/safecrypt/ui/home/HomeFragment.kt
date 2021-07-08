package com.penguinstudio.safecrypt.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.NavGraph
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.penguinstudio.safecrypt.NavGraphDirections
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.HomeTabPagerAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var pagerAdapter: HomeTabPagerAdapter
    private lateinit var viewPager: ViewPager2


    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        (activity as AppCompatActivity?)?.supportActionBar?.show()


        return inflater.inflate(R.layout.fragment_home, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pagerAdapter = HomeTabPagerAdapter(this)
        viewPager = view.findViewById(R.id.pager)

        // Disable user-swipe
        viewPager.isUserInputEnabled = false;
        viewPager.adapter = pagerAdapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when(position) {
                0 -> {
                    tab.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_baseline_image_24)
                    tab.text = "VISIBLE"
                }
                1 -> {
                    tab.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_baseline_lock_24)
                    tab.text = "INVISIBLE"
                }
            }
        }.attach()
    }
}