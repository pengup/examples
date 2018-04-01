# Hoverfly examples
Hoverfly is a very handy tool for simulating external services.

See more [hoverfly](http://hoverfly.readthedocs.io/en/latest/index.html)


## Capture a simulation

```bash
hoverctl start
hoverctl mode capture
curl --proxy http://localhost:8500 http://localhost:3000/posts/1
hoverctl export simulation.json
hoverctl stop
```

## Modify response
```bash
hoverctl start
hoverctl import simulation.json
hoverctl middleware --binary python --script middleware.py
curl --proxy http://localhost:8500 http://localhost:3000/posts/1
hoverctl stop
```