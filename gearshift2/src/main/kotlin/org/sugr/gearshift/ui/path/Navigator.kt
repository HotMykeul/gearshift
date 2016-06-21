package org.sugr.gearshift.ui.path

import android.content.SharedPreferences
import android.support.v4.app.FragmentManager
import org.sugr.gearshift.viewmodel.LeaveBlocker
import org.sugr.gearshift.viewmodel.RetainedViewModel
import org.sugr.gearshift.viewmodel.viewModelFrom

class PathNavigator(private var consumer: PathNavigator.Consumer) {
    private var viewModel =
            viewModelFrom(consumer.getSupportFragmentManager()) { tag, prefs ->
                ContentHierarchyViewModel(tag, prefs)
            }

    interface Consumer {
        val defaultPath : Path<*>
        fun getSupportFragmentManager(): FragmentManager
        fun onSetContent(newPath: Path<*>, oldPath: Path<*>)
    }

    fun restorePath() {
        setPath(viewModel.pop() ?: consumer.defaultPath)
    }

    fun setPath(path: Path<*>) {
        val current = viewModel.last() ?: consumer.defaultPath
        val currentDepth = current.depth
        val depth = path.depth

        if (currentDepth >= depth && current !== path) {
            current.destroyViewModel(consumer.getSupportFragmentManager())
        }

        consumer.onSetContent(path, current)

        if (currentDepth > depth) {
            viewModel.pop()
        } else if (currentDepth == depth && !viewModel.isEmpty()) {
            viewModel.replaceLast(path)
        } else {
            viewModel.push(path)
        }
    }

    fun navigateUp() : Boolean {
        if (viewModel.size() > 1) {
            val vm = viewModel.last()?.getViewModel(consumer.getSupportFragmentManager())
            if (vm is LeaveBlocker && !vm.canLeave()) {
                return false
            }

            setPath(viewModel.get(viewModel.size() - 2))

            return true
        }

        return false
    }
}

class ContentHierarchyViewModel(tag: String, prefs: SharedPreferences) : RetainedViewModel<Unit>(tag, prefs) {
    val contentHierarchy = mutableListOf<Path<*>>()

    fun pop() = if (contentHierarchy.size > 0) contentHierarchy.removeAt(contentHierarchy.size - 1) else null
    fun push(path: Path<*>) = contentHierarchy.add(path)
    fun replaceLast(path: Path<*>) = contentHierarchy.set(contentHierarchy.size - 1, path)
    fun get(i: Int) = contentHierarchy.get(i)
    fun last() = contentHierarchy.lastOrNull()
    fun size() = contentHierarchy.size
    fun isEmpty() = contentHierarchy.size == 0
}