package com.example.learning_test

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://cwtvssnytjzafufjqupn.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN3dHZzc255dGp6YWZ1ZmpxdXBuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI3NjE3MjQsImV4cCI6MjA3ODMzNzcyNH0.B6C3ofKh_Wi3c9TVCnWCmFi8I8gyn5rcBdAVE_UsBkU"
    ) {
        install(Postgrest)
        
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
}