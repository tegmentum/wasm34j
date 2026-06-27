;; Minimal module used by the wasm34j integration tests.
;; Exports add(i32, i32) -> i32.
(module
  (func $add (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.add)
  (export "add" (func $add)))
