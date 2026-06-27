;; Imports host functions add(i32,i32)->i32 and log(i32)->() and calls them.
(module
  (import "env" "add" (func $add (param i32 i32) (result i32)))
  (import "env" "log" (func $log (param i32)))
  (func (export "callAdd") (param i32 i32) (result i32)
    local.get 0
    local.get 1
    call $add)
  (func (export "doLog") (param i32)
    local.get 0
    call $log))
