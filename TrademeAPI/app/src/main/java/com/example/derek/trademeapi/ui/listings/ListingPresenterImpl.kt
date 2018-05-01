package com.example.derek.trademeapi.ui.listings

import com.example.derek.trademeapi.api.TradeMeApiService
import com.example.derek.trademeapi.model.Category
import com.example.derek.trademeapi.model.Listing
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by derek on 1/05/18.
 */
class ListingPresenterImpl @Inject constructor(override val view: ListingView) : ListingPresenter {

    @Inject lateinit var apiService: TradeMeApiService


    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
    private lateinit var rootCategory: Category
    private val listingList = ArrayList<Listing>(30)

    init {
        Timber.d("ListingsPresenterImpl view set: $view")
    }


    override fun onViewCreated() {
        super.onViewCreated()
        Timber.d(" apiService: $apiService")
        loadCategories()

    }

    override fun onViewDestroyed() {
        super.onViewDestroyed()
        compositeDisposable.clear()
    }

    override fun loadCategories() {
        apiService.category("0") // initial depth
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    rootCategory = it
                    Timber.d("rootCategory set: $rootCategory")
                }
                .doOnNext {
                    onSelectCategory(it)
                    view.setCurrentCategory(it)
                }
                .subscribe()
    }

    override fun onSelectCategory(currentCategory: Category) {
        loadMoreListings(20)
    }

    override fun loadMoreListings(count: Int?) {
        val currentSize = listingList.size
        view.updateListings(listingList, currentSize, listingList.size)
    }


    /** methods not currently in use */

    /** load more category on demand */
    // https://stackoverflow.com/questions/43364077/rxjava-load-items-on-demand
    // https://stackoverflow.com/questions/28176072/in-rxjava-how-to-pass-a-variable-along-when-chaining-observables
    // https://stackoverflow.com/questions/31246088/how-to-do-recursive-observable-call-in-rxjava

    fun preFetchCategory(parent: Category): Observable<Category> {
        return when {
            parent.isLeaf -> {
                Observable.just(parent)
            }
            parent.subcategories == null -> {
                apiService.category(parent.number ?: "0", depth = 5).doOnNext {
                    parent.subcategories = it.subcategories
                }
            }
            else -> {
                Observable.just(parent).flatMap {
                    Observable.fromIterable(it.subcategories)
                }.flatMap { preFetchCategory(it) }
            }
        }
    }
}