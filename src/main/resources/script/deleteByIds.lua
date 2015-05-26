local cnt = 0;
for i, v in ipairs(KEYS) do
	redis.call('del', v);
	redis.call('lrem', ARGV[1], 1, v);
	cnt = cnt + 1;
end

return cnt