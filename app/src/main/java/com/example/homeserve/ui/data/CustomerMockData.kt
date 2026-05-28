package com.example.homeserve.ui.data

data class ServiceCategory(
    val id: String,
    val name: String,
    val icon: String
)

data class ServiceItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val price: Int,
    val rating: Double,
    val reviews: Int
)

data class BookingItem(
    val id: String,
    val serviceName: String,
    val serviceId: String,
    val date: String,
    val time: String,
    val status: String,
    val address: String,
    val price: Int,
    val providerName: String? = null,
    val providerPhone: String? = null
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val read: Boolean
)

data class AddressItem(
    val id: String,
    val label: String,
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val isDefault: Boolean,
    val latitude: Double = 31.5204,
    val longitude: Double = 74.3587
)

object CustomerMockData {
    val serviceCategories = listOf(
        ServiceCategory("electrician", "Electrician", "⚡"),
        ServiceCategory("plumber", "Plumber", "🔧"),
        ServiceCategory("cleaning", "Cleaning", "✨"),
        ServiceCategory("appliance", "Appliance Repair", "🔨"),
        ServiceCategory("beauty", "Beauty Services", "💅"),
        ServiceCategory("painting", "Painting", "🎨")
    )

    val services = listOf(
        ServiceItem("elec-1", "electrician", "Fan Installation", "Professional ceiling fan installation with testing", 299, 4.7, 1234),
        ServiceItem("elec-2", "electrician", "Switch & Socket Repair", "Fix or replace faulty switches and sockets", 149, 4.8, 2156),
        ServiceItem("elec-3", "electrician", "Wiring & Rewiring", "Complete electrical wiring solutions", 499, 4.6, 856),
        ServiceItem("plumb-1", "plumber", "Tap Repair", "Fix leaking or damaged taps", 199, 4.5, 1845),
        ServiceItem("plumb-2", "plumber", "Toilet Installation", "Complete toilet installation service", 599, 4.7, 923),
        ServiceItem("plumb-3", "plumber", "Pipe Leakage", "Detect and fix all types of pipe leaks", 349, 4.8, 1567),
        ServiceItem("clean-1", "cleaning", "Deep Cleaning", "Thorough cleaning of entire home", 1499, 4.9, 3421),
        ServiceItem("clean-2", "cleaning", "Bathroom Cleaning", "Complete bathroom sanitization", 499, 4.7, 2134),
        ServiceItem("clean-3", "cleaning", "Kitchen Cleaning", "Deep kitchen cleaning and degreasing", 699, 4.8, 1876),
        ServiceItem("app-1", "appliance", "AC Repair", "Air conditioner servicing and repair", 399, 4.6, 2134),
        ServiceItem("app-2", "appliance", "Washing Machine Repair", "All brands washing machine repair", 349, 4.7, 1543),
        ServiceItem("app-3", "appliance", "Refrigerator Repair", "Fridge cooling and other issues", 449, 4.5, 1234),
        ServiceItem("beauty-1", "beauty", "Haircut & Styling", "Professional haircut at your doorstep", 299, 4.8, 3421),
        ServiceItem("beauty-2", "beauty", "Facial & Cleanup", "Premium facial treatments", 799, 4.9, 2876),
        ServiceItem("beauty-3", "beauty", "Manicure & Pedicure", "Complete nail care service", 599, 4.7, 1987),
        ServiceItem("paint-1", "painting", "Wall Painting", "Professional wall painting service", 2999, 4.6, 876),
        ServiceItem("paint-2", "painting", "Texture Painting", "Decorative texture painting", 3999, 4.7, 543)
    )

    val mockBookings = listOf(
        BookingItem("book-1", "Deep Cleaning", "clean-1", "2026-02-15", "10:00 AM - 12:00 PM", "pending", "123 Main Street, Apt 4B, New York, NY 10001", 1499),
        BookingItem("book-2", "Fan Installation", "elec-1", "2026-02-12", "2:00 PM - 4:00 PM", "accepted", "123 Main Street, Apt 4B, New York, NY 10001", 299, "John Smith", "+1 234-567-8900"),
        BookingItem("book-3", "Tap Repair", "plumb-1", "2026-02-08", "11:00 AM - 1:00 PM", "completed", "456 Oak Avenue, Brooklyn, NY 11201", 199, "Mike Johnson"),
        BookingItem("book-4", "AC Repair", "app-1", "2026-02-05", "3:00 PM - 5:00 PM", "cancelled", "123 Main Street, Apt 4B, New York, NY 10001", 399)
    )

    val notifications = listOf(
        NotificationItem("notif-1", "Booking Confirmed", "Your Deep Cleaning service has been confirmed for Feb 15, 2026", "2 hours ago", false),
        NotificationItem("notif-2", "Provider Assigned", "John Smith will be providing your Fan Installation service", "1 day ago", false),
        NotificationItem("notif-3", "Service Completed", "Your Tap Repair service has been completed. Please rate your experience", "2 days ago", true)
    )

    val mockAddresses = mutableListOf(
        AddressItem("addr-1", "Home", "Chungi Amer Sidhu, Lahore", "Lahore", "Punjab", "54000", true, 31.45036, 74.35334)
    )

    val timeSlots = listOf(
        "8:00 AM - 10:00 AM",
        "10:00 AM - 12:00 PM",
        "12:00 PM - 2:00 PM",
        "2:00 PM - 4:00 PM",
        "4:00 PM - 6:00 PM",
        "6:00 PM - 8:00 PM"
    )
}
