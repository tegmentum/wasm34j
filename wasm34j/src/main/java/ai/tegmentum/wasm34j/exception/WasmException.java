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
package ai.tegmentum.wasm34j.exception;

/**
 * Unchecked exception for all wasm34j failures: module parsing, instantiation, function lookup, and
 * invocation errors surfaced from the underlying wasm3 runtime.
 *
 * <p>This type is referenced by name from the native JNI glue, which throws it when a wasm3 {@code
 * M3Result} indicates an error. Keep its fully-qualified name in sync with the native code if it
 * ever moves.
 */
public class WasmException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WasmException(final String message) {
        super(message);
    }

    public WasmException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
