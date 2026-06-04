package com.example.homeserve.data

import kotlin.math.*

object GeohashUtils {
    private const val BASE_32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private val BITS = intArrayOf(16, 8, 4, 2, 1)

    /**
     * Encodes coordinates to a standard Geohash string.
     */
    fun encode(latitude: Double, longitude: Double, precision: Int = 9): String {
        val latRange = doubleArrayOf(-90.0, 90.0)
        val lonRange = doubleArrayOf(-180.0, 180.0)
        val geohash = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0

        while (geohash.length < precision) {
            val mid: Double
            if (isEven) {
                mid = (lonRange[0] + lonRange[1]) / 2.0
                if (longitude > mid) {
                    ch = ch or BITS[bit]
                    lonRange[0] = mid
                } else {
                    lonRange[1] = mid
                }
            } else {
                mid = (latRange[0] + latRange[1]) / 2.0
                if (latitude > mid) {
                    ch = ch or BITS[bit]
                    latRange[0] = mid
                } else {
                    latRange[1] = mid
                }
            }

            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(BASE_32[ch])
                bit = 0
                ch = 0
            }
        }
        return geohash.toString()
    }

    /**
     * Finds the longest common prefix of a list of strings.
     */
    fun commonPrefix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        var prefix = strings[0]
        for (i in 1 until strings.size) {
            while (!strings[i].startsWith(prefix)) {
                prefix = prefix.substring(0, prefix.length - 1)
                if (prefix.isEmpty()) return ""
            }
        }
        return prefix
    }

    /**
     * Calculates the bounding box around the center coordinate for a given radius in kilometers
     * and extracts the common geohash prefix of the 4 corners of that bounding box.
     */
    fun getCommonGeohashPrefix(lat: Double, lon: Double, radiusKm: Double, precision: Int = 9): String {
        if (lat == 0.0 && lon == 0.0 || radiusKm <= 0.0) return ""

        val latDiff = radiusKm / 111.0
        val lonDiff = radiusKm / (111.0 * cos(Math.toRadians(lat)))

        val minLat = lat - latDiff
        val maxLat = lat + latDiff
        val minLon = lon - lonDiff
        val maxLon = lon + lonDiff

        // 4 corners of the bounding box
        val hash1 = encode(maxLat, minLon, precision) // top-left
        val hash2 = encode(maxLat, maxLon, precision) // top-right
        val hash3 = encode(minLat, minLon, precision) // bottom-left
        val hash4 = encode(minLat, maxLon, precision) // bottom-right

        return commonPrefix(listOf(hash1, hash2, hash3, hash4))
    }
}
