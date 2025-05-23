package com.flipkart.varadhi.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HostUtilsTest {

    @BeforeAll
    public static void init() throws UnknownHostException {
        HostUtils.init();
    }

    @Test
    public void TestGetHostName() throws UnknownHostException {
        // dummy test.. no validations here.
        String host = HostUtils.getHostName();
        Assertions.assertNotNull(host);
        Assertions.assertEquals(InetAddress.getLocalHost().getHostName(), host);
    }

    @Test
    public void TestGetHostAddress() throws UnknownHostException {
        // dummy test.. no validations here.
        String address = HostUtils.getHostAddress();
        Assertions.assertNotNull(address);
        Assertions.assertEquals(InetAddress.getLocalHost().getHostAddress(), address);
    }

}
