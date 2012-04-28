/*
 * Copyright (C) 2011 Baidu.com Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.cafe.mockclient;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.baidu.cafe.local.SocketClient;

/**
 * @author zhouzheng03@baidu.com
 * 
 */
public class MockHelper {

    /**
	 * 
	 */
    public MockHelper() {
    }

    /**
     * @param filePath
     * @param caseId
     * @return
     * @throws UnknownHostException
     * @throws IOException
     */
    public String config(String filePath, String caseId) throws UnknownHostException, IOException {
        if (filePath == null || caseId == null) {
            throw new IllegalArgumentException("Invalid Argument!");
        }
        Socket sock = new Socket(MockConstant.LOCAL_HOST, MockConstant.CONTROL_PORT);
        SocketClient client = new SocketClient(sock);
        try {
            return client.sendMsg(getFilePath(filePath), getCaseId(caseId));
        } finally {
            client.close();
        }
    }

    /**
     * @param filePath
     * @return
     */
    private String getFilePath(String filePath) {
        return new StringBuilder(MockConstant.FILE_PATH_PRE).append(filePath).toString();
    }

    /**
     * @param caseId
     * @return
     */
    private String getCaseId(String caseId) {
        return new StringBuilder(MockConstant.CASE_ID_PRE).append(caseId).toString();
    }
}
