// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Start Demo") }
    var socket: java.net.Socket? = null

    MaterialTheme {
        Column {
            Row {
                val isChecked = remember { mutableStateOf(false) }
                Checkbox(
                    checked = isChecked.value,
                    onCheckedChange = {
                        System.out.println("it=" + it)
                        if (it) {
                            if (socket == null) {
                                socket = java.net.Socket("localhost", 7000);
                            }
                        } else if (socket != null) {
                            var socketToClose = socket;
                            if (null != socketToClose) {
                                socketToClose.close();
                            }
                            socket = null;
                        }
                        isChecked.value = it
                    })
                Button(onClick = {
                    var localSocket = socket;
                    if (localSocket != null) {
                        val bufferedWriter = localSocket.getOutputStream().bufferedWriter(Charsets.UTF_8)
                        bufferedWriter.write("sup dispatchStartScanAllThenContinuousDemoRevFirst\r\n")
                        bufferedWriter.flush()
                        var bufferedReader = localSocket.getInputStream().bufferedReader(Charsets.UTF_8);
                        var result = bufferedReader.readLine();
                        System.out.println("result=" + result);
                        text = result;
                        while (!result.startsWith("!!END RESPONSE ")) {
                            result = bufferedReader.readLine();
                            if (result.startsWith("----------------------")) {
                                continue;
                            }
                            System.out.println("result=" + result);
                            text = text + "\r\n" + result;
                        }
                    }

                }) {
                    Text("Start")
                }
                Button(onClick = {
                    var localSocket = socket;
                    if (localSocket != null) {
                        val bufferedWriter = localSocket.getOutputStream().bufferedWriter(Charsets.UTF_8)
                        bufferedWriter.write("sup performSafeAbortAllAction\r\n")
                        bufferedWriter.flush()
                        var bufferedReader = localSocket.getInputStream().bufferedReader(Charsets.UTF_8);
                        var result = bufferedReader.readLine();
                        System.out.println("result=" + result);
                        text = result;
                        while (!result.startsWith("!!END RESPONSE ")) {
                            result = bufferedReader.readLine();
                            if (result.startsWith("----------------------")) {
                                continue;
                            }
                            System.out.println("result=" + result);
                            text = text + "\r\n" + result;
                        }
                    }

                }) {
                    Text("Stop")
                }
            }
            Row {
                Text(text)
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
