/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.leakcanary

import im.vector.app.core.debug.LeakDetector
import leakcanary.LeakCanary
import javax.inject.Inject

class LeakCanaryLeakDetector @Inject constructor() : LeakDetector {
    override fun enable(enable: Boolean) {
        LeakCanary.config = LeakCanary.config.copy(dumpHeap = enable)
        LeakCanary.showLeakDisplayActivityLauncherIcon(false)//this line of code stop the leaks launcher icon
    }
}
