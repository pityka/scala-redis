package com.redis

import org.apache.commons.pool._
import org.apache.commons.pool.impl._


private [redis] class RedisClientFactory(host: String, port: Int) extends PoolableObjectFactory {
  // when we make an object it's already connected
  def makeObject = new RedisClient(host, port) 

  // quit & disconnect
  def destroyObject(rc: Object): Unit = { 
    rc.asInstanceOf[RedisClient].quit // need to quit for closing the connection
    rc.asInstanceOf[RedisClient].disconnect // need to disconnect for releasing sockets
  }

  // noop: we want to have it connected
  def passivateObject(rc: Object): Unit = {} 
  def validateObject(rc: Object) = rc.asInstanceOf[RedisClient].connected == true

  // noop: it should be connected already
  def activateObject(rc: Object): Unit = {}
}

class RedisClientPool(host: String, port: Int, poolSize:Int = 0) {
  val pool = if (poolSize > 0) new GenericObjectPool(new RedisClientFactory(host, port),poolSize,GenericObjectPool.WHEN_EXHAUSTED_BLOCK,-1 )
  else new StackObjectPool(new RedisClientFactory(host, port))

  override def toString = host + ":" + String.valueOf(port)

  def withClient[T](body: RedisClient => T) = {
    val client = pool.borrowObject.asInstanceOf[RedisClient]
    try {
      body(client)
    } finally {
      pool.returnObject(client)
    }
  }

  // close pool & free resources
  def close = pool.close
}
