/*
 * Copyright (c) 2026 Tegmentum AI, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasm34j.internal;

import ai.tegmentum.wasm34j.FunctionType;
import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.exception.WasmException;

/** Builds wasm3 link signatures from {@link FunctionType}s. Shared by all backends. */
public final class Signatures {

    private Signatures() {}

    /**
     * @param type the host-function type (wasm3 signatures support at most one result)
     * @return the wasm3 signature string, e.g. {@code "i(ii)"} or {@code "v()"}
     * @throws WasmException if the type has more than one result
     */
    public static String wasm3(final FunctionType type) {
        if (type.resultCount() > 1) {
            throw new WasmException("wasm3 host functions support at most one result");
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(type.resultCount() == 0 ? 'v' : type.resultTypes()[0].signatureChar());
        sb.append('(');
        for (final ValueType pt : type.parameterTypes()) {
            sb.append(pt.signatureChar());
        }
        sb.append(')');
        return sb.toString();
    }
}
