package org.oham.testredis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.SortingParams;

public class RedisClient {

	private Jedis jedis;  
	private JedisPool jedisPool;
	private ShardedJedis shardedJedis;  
	private ShardedJedisPool shardedJedisPool;
	
	
	public RedisClient() {
		initialPool();
		initialShardedPool();
		
		jedis = jedisPool.getResource();
		shardedJedis = shardedJedisPool.getResource();
	}
	
	private void initialPool() {
		JedisPoolConfig cfg  = new JedisPoolConfig();
		cfg.setMaxTotal(20);
		cfg.setMaxIdle(5);
		cfg.setMaxWaitMillis(1000L);
		cfg.setTestOnBorrow(false);
		
		jedisPool = new JedisPool(cfg, "127.0.0.1", 6379);
	}
	
	private void initialShardedPool() {
		JedisPoolConfig cfg  = new JedisPoolConfig();
		cfg.setMaxTotal(20);
		cfg.setMaxIdle(5);
		cfg.setMaxWaitMillis(1000L);
		cfg.setTestOnBorrow(false);
		
		// slave ���� 
		List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
		shards.add(new JedisShardInfo("127.0.0.1", 6379, "master"));
		
		shardedJedisPool = new ShardedJedisPool(cfg, shards);
		
	}
	
	public void keyOperate() {
		
		System.out.println("��տ���������ݣ� "+jedis.flushDB());
		System.out.println("�ж�key999���Ƿ���ڣ� "+shardedJedis.exists("key999"));
		System.out.println("���Ӽ�ֵ�� key001, value001 �� "+shardedJedis.set("key001", "value001"));
		System.out.println("�ж�key001���Ƿ���ڣ� "+shardedJedis.exists("key001"));
		System.out.println("���Ӽ�ֵ�� key002, value002 �� "+shardedJedis.set("key002", "value002"));
		System.out.println("�������м� ");
		Iterator<String> it = jedis.keys("*").iterator();
		while(it.hasNext()) {
			System.out.println("    " + it.next());
		}
		
		
		System.out.println("ɾ��key002�� "+jedis.del("key002"));
		System.out.println("�ж�key002���Ƿ���ڣ� "+shardedJedis.exists("key002"));
		System.out.println("����key001 5�����ڣ� "+jedis.expire("key001", 5));
		
		try {
			Thread.sleep(2000L);
		} catch (InterruptedException e) {}
		
		System.out.println("�鿴key001��ʣ�����ʱ�䣺 "+jedis.ttl("key001"));
		System.out.println("�Ƴ�key001��ʣ�����ʱ�䣺 "+jedis.persist("key001"));
		
		
		
		try {
			Thread.sleep(4000L);
		} catch (InterruptedException e) {}
		System.out.println("�鿴key001��ʣ�����ʱ�䣺 "+jedis.ttl("key001"));
		System.out.println("�鿴key001��ֵ���ͣ� "+jedis.type("key001"));
		
	}
	
	
	private void StringOperate() {  
        // ������ 
        System.out.println("��տ���������ݣ�"+jedis.flushDB());
        
        System.out.println("=============��=============");
        jedis.set("key001","value001");
        jedis.set("key002","value002");
        jedis.set("key003","value003");
        System.out.println("��������3����ֵ�����£�");
        System.out.println(jedis.get("key001"));
        System.out.println(jedis.get("key002"));
        System.out.println(jedis.get("key003"));
        
        System.out.println("=============ɾ=============");  
        System.out.println("ɾ��key003��ֵ�ԣ�"+jedis.del("key003"));  
        System.out.println("��ȡkey003���Ӧ��ֵ��"+jedis.get("key003"));
        
        System.out.println("=============��=============");
        //1��ֱ�Ӹ���ԭ�������
        System.out.println("ֱ�Ӹ���key001ԭ������ݣ�"+jedis.set("key001","value001-update"));
        System.out.println("��ȡkey001��Ӧ����ֵ��"+jedis.get("key001"));
        //2��ֱ�Ӹ���ԭ�������  
        System.out.println("��key002ԭ��ֵ����׷�ӣ�"+jedis.append("key002","+appendString"));
        System.out.println("��ȡkey002��Ӧ����ֵ"+jedis.get("key002")); 
   
        System.out.println("=============����ɾ���飨�����=============");
        /** 
         * mset,mgetͬʱ�������޸ģ���ѯ�����ֵ�� 
         * �ȼ��ڣ�
         * jedis.set("name","ssss"); 
         * jedis.set("jarorwar","xxxx"); 
         */  
        System.out.println("һ��������key201,key202,key203,key204�����Ӧֵ��"+jedis.mset("key201","value201",
                        "key202","value202","key203","value203","key204","value204"));  
        System.out.println("һ���Ի�ȡkey201,key202,key203,key204���Զ�Ӧ��ֵ��"+
                        jedis.mget("key201","key202","key203","key204"));
        System.out.println("һ����ɾ��key201,key202��"+jedis.del(new String[]{"key201", "key202"}));
        System.out.println("һ���Ի�ȡkey201,key202,key203,key204���Զ�Ӧ��ֵ��"+
                jedis.mget("key201","key202","key203","key204")); 
        System.out.println();
                
            
        //jedis�߱��Ĺ���shardedJedis��Ҳ��ֱ��ʹ�ã��������һЩǰ��û�ù�ķ���
        System.out.println("======================String_2=========================="); 
        // ������ 
        System.out.println("��տ���������ݣ�"+jedis.flushDB());       
       
        System.out.println("=============������ֵ��ʱ��ֹ����ԭ��ֵ=============");
        System.out.println("ԭ��key301������ʱ������key301��"+shardedJedis.setnx("key301", "value301"));
        System.out.println("ԭ��key302������ʱ������key302��"+shardedJedis.setnx("key302", "value302"));
        System.out.println("��key302����ʱ����������key302��"+shardedJedis.setnx("key302", "value302_new"));
        System.out.println("��ȡkey301��Ӧ��ֵ��"+shardedJedis.get("key301"));
        System.out.println("��ȡkey302��Ӧ��ֵ��"+shardedJedis.get("key302"));
        
        System.out.println("=============������Ч�ڼ�ֵ�Ա�ɾ��=============");
        // ����key����Ч�ڣ����洢��� 
        System.out.println("����key303����ָ������ʱ��Ϊ2��"+shardedJedis.setex("key303", 2, "key303-2second")); 
        System.out.println("��ȡkey303��Ӧ��ֵ��"+shardedJedis.get("key303")); 
        try{ 
            Thread.sleep(3000); 
        } 
        catch (InterruptedException e){ 
        } 
        System.out.println("3��֮�󣬻�ȡkey303��Ӧ��ֵ��"+shardedJedis.get("key303")); 
        
        System.out.println("=============��ȡԭֵ������Ϊ��ֵһ�����=============");
        System.out.println("key302ԭֵ��"+shardedJedis.getSet("key302", "value302-after-getset"));
        System.out.println("key302��ֵ��"+shardedJedis.get("key302"));
        
        System.out.println("=============��ȡ�Ӵ�=============");
        System.out.println("��ȡkey302��Ӧֵ�е��Ӵ���"+shardedJedis.getrange("key302", 5, 7));         
//        jedis.eval(script, keyCount, params)
    }
	
	public void ListOperate() { 
        System.out.println("======================list=========================="); 
        // ������ 
        System.out.println("��տ���������ݣ�"+jedis.flushDB()); 

        System.out.println("=============��=============");
        shardedJedis.lpush("stringlists", "vector"); 
        shardedJedis.lpush("stringlists", "ArrayList"); 
        shardedJedis.lpush("stringlists", "vector");
        shardedJedis.lpush("stringlists", "vector");
        shardedJedis.lpush("stringlists", "LinkedList");
        shardedJedis.lpush("stringlists", "MapList");
        shardedJedis.lpush("stringlists", "SerialList");
        shardedJedis.lpush("stringlists", "HashList");
        shardedJedis.lpush("numberlists", "3");
        shardedJedis.lpush("numberlists", "1");
        shardedJedis.lpush("numberlists", "5");
        shardedJedis.lpush("numberlists", "2");
        System.out.println("����Ԫ��-stringlists��"+shardedJedis.lrange("stringlists", 0, -1));
        System.out.println("����Ԫ��-numberlists��"+shardedJedis.lrange("numberlists", 0, -1));
        
        System.out.println("=============ɾ=============");
        // ɾ���б�ָ����ֵ ���ڶ�������Ϊɾ��ĸ������ظ�ʱ������add��ȥ��ֵ�ȱ�ɾ�������ڳ�ջ
        System.out.println("�ɹ�ɾ��ָ��Ԫ�ظ���-stringlists��"+shardedJedis.lrem(" ", 2, "vector")); 
        System.out.println("ɾ��ָ��Ԫ��֮��-stringlists��"+shardedJedis.lrange("stringlists", 0, -1));
        // ɾ������������� 
        System.out.println("ɾ���±�0-3���֮���Ԫ�أ�"+shardedJedis.ltrim("stringlists", 0, 3));
        System.out.println("ɾ��ָ�����֮��Ԫ�غ�-stringlists��"+shardedJedis.lrange("stringlists", 0, -1));
        // �б�Ԫ�س�ջ 
        System.out.println("��ջԪ�أ�"+shardedJedis.lpop("stringlists")); 
        System.out.println("Ԫ�س�ջ��-stringlists��"+shardedJedis.lrange("stringlists", 0, -1));
        
        System.out.println("=============��=============");
        // �޸��б���ָ���±��ֵ 
        shardedJedis.lset("stringlists", 0, "hello list!"); 
        System.out.println("�±�Ϊ0��ֵ�޸ĺ�-stringlists��"+shardedJedis.lrange("stringlists", 0, -1));
        System.out.println("=============��=============");
        // ���鳤�� 
        System.out.println("����-stringlists��"+shardedJedis.llen("stringlists"));
        System.out.println("����-numberlists��"+shardedJedis.llen("numberlists"));
        // ���� 
        /*
         * list�д��ַ�ʱ����ָ������Ϊalpha�����ʹ��SortingParams������ֱ��ʹ��sort("list")��
         * �����"ERR One or more scores can't be converted into double"
         */
        SortingParams sortingParameters = new SortingParams();
        sortingParameters.alpha();
        sortingParameters.limit(0, 3);
        System.out.println("���������Ľ��-stringlists��"+shardedJedis.sort("stringlists",sortingParameters)); 
        System.out.println("���������Ľ��-numberlists��"+shardedJedis.sort("numberlists"));
        // �Ӵ���  startΪԪ���±꣬endҲΪԪ���±ꣻ-1��?��һ��Ԫ�أ�-2��?��ڶ���Ԫ��
        System.out.println("�Ӵ�-�ڶ�����ʼ������"+shardedJedis.lrange("stringlists", 1, -1));
        // ��ȡ�б�ָ���±��ֵ 
        System.out.println("��ȡ�±�Ϊ2��Ԫ�أ�"+shardedJedis.lindex("stringlists", 2)+"\n");
    }
	
	public void SetOperate() { 

        System.out.println("======================set=========================="); 
        // ������ 
        System.out.println("��տ���������ݣ�"+jedis.flushDB());
        
        System.out.println("=============��=============");
        System.out.println("��sets�����м���Ԫ��element001��"+jedis.sadd("sets", "element001")); 
        System.out.println("��sets�����м���Ԫ��element002��"+jedis.sadd("sets", "element002")); 
        System.out.println("��sets�����м���Ԫ��element003��"+jedis.sadd("sets", "element003"));
        System.out.println("��sets�����м���Ԫ��element004��"+jedis.sadd("sets", "element004"));
        System.out.println("�鿴sets�����е�����Ԫ��:"+jedis.smembers("sets")); 
        System.out.println();
        
        System.out.println("=============ɾ=============");
        System.out.println("����sets��ɾ��Ԫ��element003��"+jedis.srem("sets", "element003"));
        System.out.println("�鿴sets�����е�����Ԫ��:"+jedis.smembers("sets"));
        System.out.println("sets����������λ�õ�Ԫ�س�ջ��"+jedis.spop("sets"));//ע����ջԪ��λ�þ�Ȼ������--��ʵ������
        System.out.println("�鿴sets�����е�����Ԫ��:"+jedis.smembers("sets"));
        System.out.println();
        
        System.out.println("=============��=============");
        System.out.println();
        
        System.out.println("=============��=============");
        System.out.println("�ж�element001�Ƿ��ڼ���sets�У�"+jedis.sismember("sets", "element001"));
        System.out.println("ѭ����ѯ��ȡsets�е�ÿ��Ԫ�أ�");
        Set<String> set = jedis.smembers("sets");   
        Iterator<String> it=set.iterator() ;   
        while(it.hasNext()){   
            Object obj=it.next();   
            System.out.println(obj);   
        }  
        System.out.println();
        
        System.out.println("=============��������=============");
        System.out.println("sets1�����Ԫ��element001��"+jedis.sadd("sets1", "element001")); 
        System.out.println("sets1�����Ԫ��element002��"+jedis.sadd("sets1", "element002")); 
        System.out.println("sets1�����Ԫ��element003��"+jedis.sadd("sets1", "element003")); 
        System.out.println("sets1�����Ԫ��element002��"+jedis.sadd("sets2", "element002")); 
        System.out.println("sets1�����Ԫ��element003��"+jedis.sadd("sets2", "element003")); 
        System.out.println("sets1�����Ԫ��element004��"+jedis.sadd("sets2", "element004"));
        System.out.println("�鿴sets1�����е�����Ԫ��:"+jedis.smembers("sets1"));
        System.out.println("�鿴sets2�����е�����Ԫ��:"+jedis.smembers("sets2"));
        System.out.println("sets1��sets2������"+jedis.sinter("sets1", "sets2"));
        System.out.println("sets1��sets2������"+jedis.sunion("sets1", "sets2"));
        System.out.println("sets1��sets2���"+jedis.sdiff("sets1", "sets2"));//���set1���У�set2��û�е�Ԫ��
        
    }
	
	public void SortedSetOperate() { 
        System.out.println("======================zset=========================="); 
        // ������ 
        System.out.println(jedis.flushDB()); 
        
        System.out.println("=============��=============");
        System.out.println("zset�����Ԫ��element001��"+shardedJedis.zadd("zset", 7.0, "element001")); 
        System.out.println("zset�����Ԫ��element002��"+shardedJedis.zadd("zset", 8.0, "element002")); 
        System.out.println("zset�����Ԫ��element003��"+shardedJedis.zadd("zset", 2.0, "element003")); 
        System.out.println("zset�����Ԫ��element004��"+shardedJedis.zadd("zset", 3.0, "element004"));
        System.out.println("zset�����е�����Ԫ�أ�"+shardedJedis.zrange("zset", 0, -1));//����Ȩ��ֵ����
        System.out.println();
        
        System.out.println("=============ɾ=============");
        System.out.println("zset��ɾ��Ԫ��element002��"+shardedJedis.zrem("zset", "element002"));
        System.out.println("zset�����е�����Ԫ�أ�"+shardedJedis.zrange("zset", 0, -1));
        System.out.println();
        
        System.out.println("=============��=============");
        System.out.println();
        
        System.out.println("=============��=============");
        System.out.println("ͳ��zset�����е�Ԫ���и���"+shardedJedis.zcard("zset"));
        System.out.println("ͳ��zset������Ȩ��ĳ����Χ�ڣ�1.0����5.0����Ԫ�صĸ���"+shardedJedis.zcount("zset", 1.0, 5.0));
        System.out.println("�鿴zset������element004��Ȩ�أ�"+shardedJedis.zscore("zset", "element004"));
        System.out.println("�鿴�±�1��2��Χ�ڵ�Ԫ��ֵ��"+shardedJedis.zrange("zset", 1, 2));

    }
	
	public void HashOperate() 
    { 
        System.out.println("======================hash==========================");
        //������ 
        System.out.println(jedis.flushDB()); 
        
        System.out.println("=============��=============");
        System.out.println("hashs�����key001��value001��ֵ�ԣ�"+shardedJedis.hset("hashs", "key001", "value001")); 
        System.out.println("hashs�����key002��value002��ֵ�ԣ�"+shardedJedis.hset("hashs", "key002", "value002")); 
        System.out.println("hashs�����key003��value003��ֵ�ԣ�"+shardedJedis.hset("hashs", "key003", "value003"));
        System.out.println("����key004��4�����ͼ�ֵ�ԣ�"+shardedJedis.hincrBy("hashs", "key004", 4l));
        System.out.println("hashs�е�����ֵ��"+shardedJedis.hvals("hashs"));
        System.out.println();
        
        System.out.println("=============ɾ=============");
        System.out.println("hashs��ɾ��key002��ֵ�ԣ�"+shardedJedis.hdel("hashs", "key002"));
        System.out.println("hashs�е�����ֵ��"+shardedJedis.hvals("hashs"));
        System.out.println();
        
        System.out.println("=============��=============");
        System.out.println("key004���ͼ�ֵ��ֵ����100��"+shardedJedis.hincrBy("hashs", "key004", 100l));
        System.out.println("hashs�е�����ֵ��"+shardedJedis.hvals("hashs"));
        System.out.println();
        
        System.out.println("=============��=============");
        System.out.println("�ж�key003�Ƿ���ڣ�"+shardedJedis.hexists("hashs", "key003"));
        System.out.println("��ȡkey004��Ӧ��ֵ��"+shardedJedis.hget("hashs", "key004"));
        System.out.println("������ȡkey001��key003��Ӧ��ֵ��"+shardedJedis.hmget("hashs", "key001", "key003")); 
        System.out.println("��ȡhashs�����е�key��"+shardedJedis.hkeys("hashs"));
        System.out.println("��ȡhashs�����е�value��"+shardedJedis.hvals("hashs"));
        System.out.println();
        

    }
	
	
	public void testSetCal(){
		long bookId = 0l;
		
		jedis.flushDB();
		
		
		jedis.incr("book_id");
		
		bookId = Long.parseLong(jedis.get("book_id"));
		
		StringBuilder sb = new StringBuilder("book:"+bookId);
		jedis.hset(sb.toString(), "name", "The Ruby Programming Language");
		jedis.hset(sb.toString(), "author", "Mark Pilgrim");
		sb.delete(0, sb.length());
		
		jedis.incr("book_id");
		bookId = Long.parseLong(jedis.get("book_id"));
		sb.append("book:").append(bookId);
		
		jedis.hset(sb.toString(), "name", "Ruby on rail");
		jedis.hset(sb.toString(), "author", "David Flanagan");
		
		sb.delete(0, sb.length());
		
		jedis.incr("book_id");
		bookId = Long.parseLong(jedis.get("book_id"));
		sb.append("book:").append(bookId);
		
		jedis.hset(sb.toString(), "name", "Programming Erlang");
		jedis.hset(sb.toString(), "author", "Joe Armstrong");
		
		
		
		jedis.sadd("tag:ruby", "1");
		jedis.sadd("tag:ruby", "2");
		jedis.sadd("tag:web", "2");
		jedis.sadd("tag:erlang", "3");
		
		Set<String> booKeys = jedis.sinter("tag:ruby", "tag:web"); 
		System.out.println("=========  web & ruby  ========");
		for(String k : booKeys) {
			System.out.println(jedis.hget("book:"+k, "name") + "   " + jedis.hget("book:"+k, "author"));
		}
		System.out.println("===============================");
		
		booKeys = jedis.sdiff("tag:ruby", "tag:web"); 
		System.out.println("=========  web diff ruby  ========");
		for(String k : booKeys) {
			System.out.println(jedis.hget("book:"+k, "name") + "   " + jedis.hget("book:"+k, "author"));
		}
		System.out.println("===============================");
		
		booKeys = jedis.sunion("tag:ruby", "tag:web"); 
		System.out.println("=========  web union ruby  ========");
		for(String k : booKeys) {
			System.out.println(jedis.hget("book:"+k, "name") + "   " + jedis.hget("book:"+k, "author"));
		}
		System.out.println("===============================");
		
	}
	
	
	public void testSort() {
		SortingParams sortingParameters1 = new SortingParams();
		sortingParameters1.by("name*").get("name*").get("#").alpha();
		
		
		SortingParams sortingParams2 = new SortingParams();
		sortingParams2.get("user*->name");
		
		
		List<String> list = jedis.sort("ml", sortingParameters1);
		System.out.println(list);
		
		
		
	}
	
	public static void main(String[] args) {
		RedisClient test = new RedisClient();
		test.testSort();
		
		/*System.out.println(Arrays.toString("|DF|A3".split("|")));
		System.out.println('|');
		System.out.println((byte)'|');
		System.out.println((byte)'D');
		System.out.println((byte)'F');
		System.out.println((byte)'A');
		System.out.println((byte)'3');
		System.out.println(Pattern.compile("|").split("|DF|A3", 0).length);
		
		char ch = 0;
		String regex = "|";
		  if (((regex.toCharArray().length == 1 &&
		             ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
		             (regex.length() == 2 &&
		              regex.charAt(0) == '\\' &&
		              (((ch = regex.charAt(1))-'0')|('9'-ch)) < 0 &&
		              ((ch-'a')|('z'-ch)) < 0 &&
		              ((ch-'A')|('Z'-ch)) < 0)) &&
		            (ch < Character.MIN_HIGH_SURROGATE ||
		             ch > Character.MAX_LOW_SURROGATE)){
			  System.out.println("---");
		  }*/
	}
	
}
