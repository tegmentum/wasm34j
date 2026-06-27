;; Exports a mutable and an immutable global for the globals tests.
(module
  (global (export "counter") (mut i32) (i32.const 7))
  (global (export "constant") i32 (i32.const 99))
  (func (export "getCounter") (result i32)
    global.get 0))
