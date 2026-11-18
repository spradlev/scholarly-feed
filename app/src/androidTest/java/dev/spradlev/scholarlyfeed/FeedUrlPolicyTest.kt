// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import java.net.InetAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedUrlPolicyTest {
    @Test
    fun rejectsNonHttpsBeforeMakingARequest() = runBlocking {
        try {
            SafeFeedFetcher().fetch("http://example.org/feed.xml")
            throw AssertionError("Expected HTTP feed URL to be rejected")
        } catch (exception: FeedFetchException) {
            assertTrue(exception.message!!.contains("HTTPS"))
        }
    }

    @Test
    fun permitsPublicAddressesAndRejectsLocalOrReservedAddresses() {
        assertTrue(address("1.1.1.1").isPublicAddress())
        assertTrue(address("2606:4700:4700::1111").isPublicAddress())
        assertFalse(address("127.0.0.1").isPublicAddress())
        assertFalse(address("10.0.0.1").isPublicAddress())
        assertFalse(address("169.254.10.1").isPublicAddress())
        assertFalse(address("100.64.0.1").isPublicAddress())
        assertFalse(address("fc00::1").isPublicAddress())
        assertFalse(address("fe80::1").isPublicAddress())
        assertFalse(address("2001:db8::1").isPublicAddress())
        assertFalse(address("2002::1").isPublicAddress())
        assertFalse(address("::ffff:127.0.0.1").isPublicAddress())
    }

    private fun address(value: String): InetAddress = InetAddress.getByName(value)
}
