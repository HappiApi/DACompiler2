# Shows a nice side-by-side diff of optimised vs unoptimised file.

# For some reason I get an error when I try to execute this, these are here
# just so I can copy and paste them.

TEST_FILE="DynamicVariableFolding.class"; icdiff <(javap -c -verbose "build/classes/comp207p/target/$TEST_FILE") <(javap -c -verbose "optimised/classes/comp207p/target/$TEST_FILE")
