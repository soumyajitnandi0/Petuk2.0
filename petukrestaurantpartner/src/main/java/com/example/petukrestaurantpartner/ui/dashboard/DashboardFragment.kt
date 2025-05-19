package com.example.petukrestaurantpartner.ui.dashboard

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petukrestaurantpartner.R
import com.example.petukrestaurantpartner.databinding.FragmentDashboardBinding
import com.example.petukrestaurantpartner.ui.dashboard.adapters.RecentOrdersAdapter
import com.example.petukrestaurantpartner.ui.dashboard.models.DashboardSummary
import com.example.petukrestaurantpartner.ui.dashboard.models.MenuItemStat
import com.example.petukrestaurantpartner.ui.dashboard.models.Order
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var recentOrdersAdapter: RecentOrdersAdapter

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupSwipeRefresh()
        setupDateRangeButtons()
        setupRecentOrdersList()
        setupViewAllOrdersButton()
        setupObservers()

        return root
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Refresh dashboard data
            dashboardViewModel.loadDashboardData()
        }

        // Set the colors used in the progress animation
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.accent,
            R.color.primary_dark
        )
    }

    private fun setupDateRangeButtons() {
        // Set initial date range buttons text
        updateDateButtonsText()

        // Setup from date button
        binding.btnDateFrom.setOnClickListener {
            showDatePicker(true)
        }

        // Setup to date button
        binding.btnDateTo.setOnClickListener {
            showDatePicker(false)
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)

            if (isStartDate) {
                val endCalendar = Calendar.getInstance()
                if (dashboardViewModel.getFormattedEndDate() != "Not set") {
                    val endDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .parse(dashboardViewModel.getFormattedEndDate())
                    endDate?.let {
                        endCalendar.time = it
                    }
                }

                if (selectedCalendar.after(endCalendar)) {
                    Toast.makeText(context, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
                    return@OnDateSetListener
                }

                val startCalendar = Calendar.getInstance()
                startCalendar.set(year, month, dayOfMonth)
                dashboardViewModel.setDateRange(startCalendar, endCalendar)
            } else {
                val startCalendar = Calendar.getInstance()
                if (dashboardViewModel.getFormattedStartDate() != "Not set") {
                    val startDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .parse(dashboardViewModel.getFormattedStartDate())
                    startDate?.let {
                        startCalendar.time = it
                    }
                }

                if (selectedCalendar.before(startCalendar)) {
                    Toast.makeText(context, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                    return@OnDateSetListener
                }

                val endCalendar = Calendar.getInstance()
                endCalendar.set(year, month, dayOfMonth)
                dashboardViewModel.setDateRange(startCalendar, endCalendar)
            }

            updateDateButtonsText()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButtonsText() {
        binding.btnDateFrom.text = "From: ${dashboardViewModel.getFormattedStartDate()}"
        binding.btnDateTo.text = "To: ${dashboardViewModel.getFormattedEndDate()}"
    }

    private fun setupRecentOrdersList() {
        recentOrdersAdapter = RecentOrdersAdapter(emptyList()) { order ->
            navigateToOrderDetails(order)
        }

        binding.recyclerRecentOrders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentOrdersAdapter
        }
    }

    private fun navigateToOrderDetails(order: Order) {
        // Navigate to order details screen
        // This would typically be implemented to navigate to an order details fragment
        // Example:
        // val action = DashboardFragmentDirections.actionDashboardToOrderDetails(order.orderId)
        // findNavController().navigate(action)

        // For now, just show a toast with the order ID
        Toast.makeText(context, "Order details: ${order.orderId}", Toast.LENGTH_SHORT).show()
    }

    private fun setupViewAllOrdersButton() {
        binding.btnViewAllOrders.setOnClickListener {
            // Navigate to orders list screen
            // Example:
            // findNavController().navigate(R.id.action_dashboard_to_orders_list)

            // For now, just show a toast
            Toast.makeText(context, "View all orders clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Observe dashboard summary data
        dashboardViewModel.dashboardSummary.observe(viewLifecycleOwner) { summary ->
            updateDashboardUI(summary)
        }

        // Observe loading state
        dashboardViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        // Observe error messages
        dashboardViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateDashboardUI(summary: DashboardSummary) {
        // Update statistics
        binding.textTotalOrders.text = summary.totalOrders.toString()
        binding.textRevenue.text = currencyFormat.format(summary.totalRevenue)
        binding.textAvgOrder.text = currencyFormat.format(summary.averageOrderValue)

        // Update recent orders
        recentOrdersAdapter.updateOrders(summary.recentOrders)

        // Update charts
        setupPieChart(summary.topSellingItems)
        setupLineChart(summary.revenueByDate)
    }

    private fun setupPieChart(topSellingItems: List<MenuItemStat>) {
        // Clear existing data
        binding.pieChart.clear()

        if (topSellingItems.isEmpty()) {
            binding.pieChart.setNoDataText("No data available")
            binding.pieChart.invalidate()
            return
        }

        // Prepare pie chart entries
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        topSellingItems.forEach { item ->
            entries.add(PieEntry(item.totalQuantity.toFloat(), item.name))
            colors.add(getRandomColor())
        }

        val dataSet = PieDataSet(entries, "Top Selling Items")
        dataSet.colors = colors
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f
        dataSet.iconsOffset = MPPointF(0f, 40f)

        val pieData = PieData(dataSet)

        // Configure pie chart
        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            legend.isEnabled = true
            setUsePercentValues(false)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            centerText = "Top Items"
            setCenterTextSize(16f)
            setCenterTextColor(Color.BLACK)
            animateY(1000)
            invalidate()
        }
    }

    private fun setupLineChart(revenueData: List<com.example.petukrestaurantpartner.ui.dashboard.models.RevenueDataPoint>) {
        // Clear existing data
        binding.lineChart.clear()

        if (revenueData.isEmpty()) {
            binding.lineChart.setNoDataText("No data available")
            binding.lineChart.invalidate()
            return
        }

        // Prepare line chart entries
        val entries = ArrayList<Entry>()
        val dates = ArrayList<String>()

        revenueData.forEachIndexed { index, dataPoint ->
            entries.add(Entry(index.toFloat(), dataPoint.revenue.toFloat()))
            dates.add(dataPoint.date)
        }

        val dataSet = LineDataSet(entries, "Revenue")
        dataSet.apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            lineWidth = 2f
            setCircleColor(Color.BLUE)
            circleRadius = 4f
            setDrawCircleHole(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(dataSet)

        // Configure line chart
        binding.lineChart.apply {
            data = lineData

            val description = Description()
            description.text = ""
            this.description = description

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(dates)
            xAxis.labelRotationAngle = -45f

            axisRight.isEnabled = false

            legend.isEnabled = true

            setPinchZoom(true)
            animateX(1000)

            invalidate()
        }
    }

    private fun getRandomColor(): Int {
        val colors = ColorTemplate.MATERIAL_COLORS + ColorTemplate.VORDIPLOM_COLORS
        return colors[Random().nextInt(colors.size)]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}