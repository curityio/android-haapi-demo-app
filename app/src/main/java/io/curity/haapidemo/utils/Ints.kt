/*
 *  Copyright (C) 2021 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.haapidemo.utils

import android.content.res.Resources

/**
 * Computes this [Int] value to a Dpi as [Int] by multiplying [resources].displayMetrics.density
 *
 * @param resources Resources
 * @return Int An integer that represents a Dpi
 */
fun Int.toDpi(resources: Resources): Int {
    return (resources.displayMetrics.density * this).toInt()
}