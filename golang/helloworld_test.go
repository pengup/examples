package main

import "testing"

func TestHelloworld(t *testing.T) {
    str := hw() 
    if str != "Hello World" {
       t.Error("Should print Hello World", str)
    }
}
