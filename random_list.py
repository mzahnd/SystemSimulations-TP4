#!/usr/bin/python

import random


def random_floats(start: float, end: float, n: int) -> list[float]:
    """Return a list with n random floats in the interval [start, end)."""
    return [random.uniform(start, end) for _ in range(n)]


# example: 10 random floats between 1 and 20
numbers = random_floats(1, 20, 20)
numbers.sort()

for number in numbers:
    print(number)
