import * as filters from '@/shared/variantFilters'

describe('variantFilters', () => {
  describe('rangeTextFilter, e.g. 156-173', () => {
    it.each`
      input          | value        | expResult
      ${null}        | ${152}       | ${true}
      ${undefined}   | ${152}       | ${true}
      ${'152'}       | ${152}       | ${true}
      ${'152-'}      | ${152}       | ${true}
      ${'-152'}      | ${152}       | ${true}
      ${'152-152'}   | ${152}       | ${true}
      ${'151-153'}   | ${152}       | ${true}
      ${'-'}         | ${152}       | ${true}
      ${'-151'}      | ${152}       | ${false}
      ${'153-'}      | ${152}       | ${false}
      ${'151'}       | ${152}       | ${false}
      ${'0.01'}      | ${0.01}      | ${true}
      ${'0.1-0.2'}   | ${0.15}      | ${true}
      ${'0.03-0.03'} | ${0.03}      | ${true}
      ${'0.01-0.1'}  | ${0.05}      | ${true}
      ${'0.01-0.1'}  | ${0.11}      | ${false}
      ${'152'}       | ${null}      | ${false}
      ${'152'}       | ${undefined} | ${false}
    `(
      'rangeTextFilter($input, $value) is $expResult',
      ({ input, value, expResult }) => {
        expect(filters.rangeTextFilter(input, value)).toBe(expResult)
      }
    )
  })

  describe('iContainsFilter', () => {
    it.each`
      input        | value        | expResult
      ${null}      | ${'hello'}   | ${true}
      ${undefined} | ${'hello'}   | ${true}
      ${'hello'}   | ${'hello'}   | ${true}
      ${'hello'}   | ${null}      | ${true}
      ${'hello'}   | ${undefined} | ${true}
      ${'hello'}   | ${''}        | ${true}
      ${'ElL'}     | ${'hello'}   | ${true}
      ${'hello'}   | ${'ElL'}     | ${false}
    `(
      'iContainsFilter($input, $value) is $expResult',
      ({ input, value, expResult }) => {
        expect(filters.iContainsFilter(input, value)).toBe(expResult)
      }
    )
  })

  describe('inSetFilter', () => {
    it.each`
      input             | value    | expResult
      ${null}           | ${'INS'} | ${true}
      ${undefined}      | ${'INS'} | ${true}
      ${[]}             | ${'INS'} | ${true}
      ${['INS']}        | ${'INS'} | ${true}
      ${['DEL', 'INS']} | ${'INS'} | ${true}
      ${['DEL', 'SNP']} | ${'INS'} | ${false}
    `(
      'inSetFilter($input, $value) is $expResult',
      ({ input, value, expResult }) => {
        expect(filters.inSetFilter(input, value)).toBe(expResult)
      }
    )
  })
})