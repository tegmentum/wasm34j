package ai.tegmentum.wasm34j.internal;

import ai.tegmentum.wasm34j.FunctionType;
import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.exception.WasmException;

/**
 * Builds wasm3 link signatures from {@link FunctionType}s. Shared by all backends.
 */
public final class Signatures {

    private Signatures() {
    }

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
