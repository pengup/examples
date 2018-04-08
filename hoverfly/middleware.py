#!/usr/bin/env python

import sys
import json
import logging
import random
from io import StringIO

logging.basicConfig(filename='middleware.log', level=logging.DEBUG)
logging.debug('Middleware "modify_request" called')


def main():
    payload = sys.stdin.readlines()[0]

    logging.debug(payload)

    payload_dict = json.loads(payload)
    # payload_dict['response']['status'] = random.choice([200, 201])

    if "response" in payload_dict and "body" in payload_dict["response"]:
        # payload_dict["response"]["body"] = "{'foo': 'baz'}\n"
        body = payload_dict["response"]["body"]
        if body != "ok":
            body_json = json.loads(body)
            id = body_json["id"]
            title = body_json["title"]
            body_json["summary"] = "Summary " + str(id) + " " + title
            payload_dict["response"]["body"] = json.dumps(body_json)
            logging.debug(payload_dict)


    print(json.dumps(payload_dict))


if __name__ == "__main__":
    main()
