package com.example.retailpos.auth

import com.example.retailpos.data.local.entity.UserEntity

enum class UserRole {
    OWNER,
    MANAGER,
    CASHIER,
    UNKNOWN;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.uppercase()) {
                "OWNER" -> OWNER
                "MANAGER" -> MANAGER
                "CASHIER" -> CASHIER
                else -> UNKNOWN
            }
        }
    }
}

object UserPermissions {
    fun canManageStaff(role: UserRole): Boolean = role == UserRole.OWNER
    
    fun canUpdateStoreProfile(role: UserRole): Boolean = role == UserRole.OWNER
    
    fun canAccessInventory(role: UserRole): Boolean = 
        role == UserRole.OWNER || role == UserRole.MANAGER
        
    fun canAdjustStock(role: UserRole): Boolean = 
        role == UserRole.OWNER || role == UserRole.MANAGER
        
    fun canAccessAnalytics(role: UserRole): Boolean = 
        role == UserRole.OWNER || role == UserRole.MANAGER
        
    fun canManageProducts(role: UserRole): Boolean = 
        role == UserRole.OWNER || role == UserRole.MANAGER

    fun canManageCustomers(role: UserRole): Boolean = true
    
    fun canPerformBilling(role: UserRole): Boolean = true
}

val UserEntity?.userRole: UserRole
    get() = UserRole.fromString(this?.role)
