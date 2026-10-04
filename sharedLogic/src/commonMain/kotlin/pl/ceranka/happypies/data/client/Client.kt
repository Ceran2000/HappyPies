package pl.ceranka.happypies.data.client

data class Dog(val name: String, val breed: String)

data class Client(val id: String, val name: String, val email: String, val dogs: List<Dog>)