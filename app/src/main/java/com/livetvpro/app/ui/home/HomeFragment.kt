package com.livetvpro.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.livetvpro.app.R
import com.livetvpro.app.SearchableFragment
import com.livetvpro.app.data.models.ListenerConfig
import com.livetvpro.app.databinding.FragmentHomeBinding
import com.livetvpro.app.ui.adapters.CategoryAdapter
import com.livetvpro.app.utils.NativeListenerManager
import com.livetvpro.app.utils.RedirectCooldownManager
import com.livetvpro.app.utils.RetryHandler
import com.livetvpro.app.utils.Refreshable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class HomeFragment : Fragment(), SearchableFragment, Refreshable {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter
    @Inject
    lateinit var listenerManager: NativeListenerManager
    @Inject
    lateinit var cooldownManager: RedirectCooldownManager
    override fun onSearchQuery(query: String) {
        viewModel.searchCategories(query)
    }
    override fun refreshData() {
        viewModel.refresh()
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val columnCount = resources.getInteger(R.integer.grid_column_count)
        (binding.recyclerViewCategories.layoutManager as? GridLayoutManager)?.spanCount = columnCount
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupRetryHandling()
        if (com.livetvpro.app.utils.DeviceUtils.isTvDevice) {
            binding.swipeRefresh.isEnabled = false
        }
    }
    override fun onResume() {
        super.onResume()
    }
    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            val bundle = bundleOf(
                "categoryId" to category.id,
                "categoryName" to category.name
            )
            cooldownManager.tryFire(ListenerConfig.PAGE_HOME, category.id) {
                listenerManager.onPageInteraction(
                    pageType = ListenerConfig.PAGE_HOME,
                    uniqueId = category.id
                )
            }
            findNavController().navigate(R.id.action_home_to_category, bundle)
        }
        val columnCount = resources.getInteger(R.integer.grid_column_count)
        binding.recyclerViewCategories.apply {
            layoutManager = GridLayoutManager(context, columnCount)
            adapter = categoryAdapter
            setHasFixedSize(true)
        }
    }
    private fun setupRetryHandling() {
        RetryHandler.setupGlobal(
            lifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
            activity = requireActivity() as androidx.appcompat.app.AppCompatActivity,
            contentView = binding.swipeRefresh,
            swipeRefresh = binding.swipeRefresh,
            progressBar = binding.progressBar,
            emptyView = binding.emptyView
        )
        viewModel.filteredCategories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
            if (com.livetvpro.app.utils.DeviceUtils.isTvDevice && categories.isNotEmpty()) {
                binding.recyclerViewCategories.post {
                    binding.recyclerViewCategories
                        .findViewHolderForAdapterPosition(0)
                        ?.itemView
                        ?.requestFocus()
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
