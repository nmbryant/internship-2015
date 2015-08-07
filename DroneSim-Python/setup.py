from setuptools import setup, find_packages

setup(
    name="DroneSim",
    version="0.1",
    description="Python scripts for drone sim",
    packages=find_packages(),
    install_requires=["pyowm>=2.2.1",
                      "dweepy>=0.2.0"],
    entry_points={[]}
)