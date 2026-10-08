package com.rb.background

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rb.background.features.home.ui.HomeFragment
import com.rb.background.features.create.ui.CreateFragment
import com.rb.background.features.history.ui.HistoryFragment
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottomNavigation
        )

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            HomeFragment()
                        )
                        .commit()
                    true
                }

                R.id.nav_create -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            CreateFragment()
                        )
                        .commit()
                    true
                }

                R.id.nav_history -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            HistoryFragment()
                        )
                        .commit()
                    true
                }

                else -> false
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    HomeFragment()
                )
                .commit()
        }
    }
}