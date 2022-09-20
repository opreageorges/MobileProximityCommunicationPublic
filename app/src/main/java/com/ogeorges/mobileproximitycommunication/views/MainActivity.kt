package com.ogeorges.mobileproximitycommunication.views

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.databinding.ActivityMainBinding
import com.ogeorges.mobileproximitycommunication.vmodels.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    // TODO ask user to change permission if not granted
    @CallSuper
    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return true
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    @CallSuper
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val errMsg = "Cannot start without required permissions"
        if (requestCode == 1) {
            grantResults.forEach {
                if (it == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
            }
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        viewModel = MainViewModel(getSharedPreferences("userPreferences", Context.MODE_PRIVATE), this)

        binding.viewModel = viewModel
        binding.executePendingBindings()

        viewModel.setInitialFragment(supportFragmentManager)

        setContentView(binding.root)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.main_menu, menu)

        return true
    }

    override fun onPause() {
        viewModel.stop()
        super.onPause()
    }


//    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
//        R.id.action_settings -> {
////            val ef = ExperimentalFragment()
////            ef.check()
//            val newFragment: Fragment = SettingsFragment()
//            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
//            transaction.replace(R.id.fragmentContainerView, newFragment)
//            transaction.addToBackStack(null)
//            transaction.commit()
//            true
//        }
//        R.id.action_main -> {
//            val newFragment: Fragment = MainFragment()
//            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
//            transaction.replace(R.id.fragmentContainerView, newFragment)
//            transaction.addToBackStack(null)
//            transaction.commit()
//            true
//        }
//        R.id.action_publish ->{
//            publish()
//            true
//        }
//
//        else -> {
//            super.onOptionsItemSelected(item)
//        }
//    }
//    private val message = Message(Build.MODEL.toByteArray())
//    private val PUB_SUB_STRATEGY = Strategy.Builder().setTtlSeconds(120).build()
//
//    private fun publish() {
//        val options = PublishOptions.Builder()
//            .setStrategy(PUB_SUB_STRATEGY)
//            .setCallback(object : PublishCallback(){}).build()
//
//        Nearby.getMessagesClient(this).publish(message, options)
//    }
//
//    private fun unpublish() {
//        Nearby.getMessagesClient(this).unpublish(message)
//    }

}