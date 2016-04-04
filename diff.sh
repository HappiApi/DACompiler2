# Shows a nice side-by-side diff of optimised vs unoptimised file.

# For some reason I get an error when I try to execute this, these are here
# just so I can copy and paste them.

diff --side-by-side <(javap -c -verbose build/classes/comp207p/target/SimpleFolding.class) <(javap -c -verbose optimised/classes/comp207p/target/SimpleFolding.class)
diff --side-by-side <(javap -c -verbose build/classes/comp207p/target/ConstantVariableFolding.class) <(javap -c -verbose optimised/classes/comp207p/target/ConstantVariableFolding.class)
diff --side-by-side <(javap -c -verbose build/classes/comp207p/target/DynamicVariableFolding.class) <(javap -c -verbose optimised/classes/comp207p/target/DynamicVariableFolding.class)
