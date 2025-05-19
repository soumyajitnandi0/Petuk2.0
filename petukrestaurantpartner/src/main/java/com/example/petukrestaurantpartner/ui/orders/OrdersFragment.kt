package com.example.petukrestaurantpartner.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.petukrestaurantpartner.databinding.FragmentOrdersBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import timber.log.Timber

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersViewModel: OrdersViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ordersViewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
            .get(OrdersViewModel::class.java)

        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewPager and TabLayout
        viewPager = binding.viewPager
        tabLayout = binding.tabLayout

        setupViewPager()
        setupTabLayout()
        observeTabNavigation()

        return root
    }

    private fun setupViewPager() {
        // Create adapter for the ViewPager
        val orderTabsAdapter = OrderTabsAdapter(this)
        viewPager.adapter = orderTabsAdapter

        // Prevent swiping between tabs too quickly
        viewPager.isUserInputEnabled = true
    }

    private fun setupTabLayout() {
        // Connect TabLayout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Live Orders"
                1 -> "Prepared"
                2 -> "Delivered"
                else -> "Unknown"
            }
        }.attach()
    }

    private fun observeTabNavigation() {
        // Observe tab navigation events from the ViewModel
        ordersViewModel.tabNavigationEvent.observe(viewLifecycleOwner) { tabIndex ->
            tabIndex?.let {
                // Navigate to the specified tab
                viewPager.setCurrentItem(it, true)
                // Clear the navigation event
                ordersViewModel.clearTabNavigationEvent()

                Timber.d("Navigating to tab index: $it")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}